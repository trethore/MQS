import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const URL_ATTRIBUTES = ['href', 'src', 'component-url', 'renderer-url', 'before-hydration-url'];
const ABSOLUTE_URL_REGEX = new RegExp(`\\b(${URL_ATTRIBUTES.join('|')})=(['\"])\\/(?!\\/)([^'\"]*)\\2`, 'g');
const GENERIC_URL_REGEX = new RegExp(`\\b(${URL_ATTRIBUTES.join('|')})=(['\"])([^'\"]*)\\2`, 'g');
const URL_SPLIT_REGEX = /^([^?#]*)([?#].*)?$/;
const SCHEME_REGEX = /^[a-zA-Z][a-zA-Z\d+.-]*:/;
const MAX_FILE_CONCURRENCY = 32;

function toPosixPath(filePath) {
  return filePath.split(path.sep).join('/');
}

function hasFileExtension(pathname) {
  const lastSegment = pathname.split('/').pop() ?? '';
  return lastSegment.includes('.');
}

function normalizeLinkPath(pathname, attribute) {
  const normalizedPathname = pathname === '' ? '' : path.posix.normalize(pathname).replace(/^\/+/, '');
  const normalized = normalizedPathname === '.' ? '' : normalizedPathname;

  if (attribute !== 'href') {
    return normalized;
  }

  if (normalized === '') {
    return 'index.html';
  }

  if (normalized.endsWith('/')) {
    return `${normalized}index.html`;
  }

  if (!hasFileExtension(normalized)) {
    return `${normalized}.html`;
  }

  return normalized;
}

function absolutishUrlToRelative(urlPath, attribute, relativeHtmlPath) {
  const [pathname, suffix = ''] = urlPath.match(URL_SPLIT_REGEX)?.slice(1) ?? [urlPath, ''];
  const normalizedTarget = normalizeLinkPath(pathname, attribute);
  const htmlDirectory = path.posix.dirname(relativeHtmlPath);
  let relativeTarget = path.posix.relative(htmlDirectory, normalizedTarget);

  if (relativeTarget === '') {
    relativeTarget = path.posix.basename(relativeHtmlPath);
  }

  if (!relativeTarget.startsWith('.')) {
    relativeTarget = `./${relativeTarget}`;
  }

  return `${relativeTarget}${suffix}`;
}

function rewriteAbsoluteUrlsToRelative(htmlContent, relativeHtmlPath) {
  return htmlContent.replace(ABSOLUTE_URL_REGEX, (match, attribute, quote, urlPath) => {
    const rewritten = absolutishUrlToRelative(urlPath, attribute, relativeHtmlPath);
    return `${attribute}=${quote}${rewritten}${quote}`;
  });
}

function shouldLeaveUrlUnchanged(urlPath) {
  return (
    urlPath === '' ||
    urlPath.startsWith('/') ||
    urlPath.startsWith('#') ||
    urlPath.startsWith('?') ||
    urlPath.startsWith('//') ||
    SCHEME_REGEX.test(urlPath)
  );
}

function rebaseRelativeUrls(htmlContent, sourceRelativeHtmlPath, destinationRelativeHtmlPath) {
  return htmlContent.replace(GENERIC_URL_REGEX, (match, attribute, quote, urlPath) => {
    if (shouldLeaveUrlUnchanged(urlPath)) {
      return match;
    }

    const [pathname, suffix = ''] = urlPath.match(URL_SPLIT_REGEX)?.slice(1) ?? [urlPath, ''];
    const sourceDirectory = path.posix.dirname(sourceRelativeHtmlPath);
    const destinationDirectory = path.posix.dirname(destinationRelativeHtmlPath);
    const absoluteTarget = path.posix.normalize(path.posix.join(sourceDirectory, pathname));
    let rebasedTarget = path.posix.relative(destinationDirectory, absoluteTarget);

    if (rebasedTarget === '') {
      rebasedTarget = path.posix.basename(destinationRelativeHtmlPath);
    }

    if (!rebasedTarget.startsWith('.')) {
      rebasedTarget = `./${rebasedTarget}`;
    }

    return `${attribute}=${quote}${rebasedTarget}${suffix}${quote}`;
  });
}

async function listHtmlFiles(directory) {
  const directoryStack = [directory];
  const htmlFiles = [];

  while (directoryStack.length > 0) {
    const currentDirectory = directoryStack.pop();
    if (currentDirectory === undefined) {
      continue;
    }

    const entries = await fs.readdir(currentDirectory, { withFileTypes: true });

    for (const entry of entries) {
      const fullPath = path.join(currentDirectory, entry.name);

      if (entry.isDirectory()) {
        directoryStack.push(fullPath);
        continue;
      }

      if (entry.isFile() && entry.name.endsWith('.html')) {
        htmlFiles.push(fullPath);
      }
    }
  }

  return htmlFiles;
}

async function mapWithConcurrency(items, mapper, maxConcurrency = MAX_FILE_CONCURRENCY) {
  if (items.length === 0) {
    return [];
  }

  const results = new Array(items.length);
  let nextIndex = 0;

  async function worker() {
    while (true) {
      const currentIndex = nextIndex;
      nextIndex += 1;

      if (currentIndex >= items.length) {
        return;
      }

      results[currentIndex] = await mapper(items[currentIndex], currentIndex);
    }
  }

  const workerCount = Math.min(maxConcurrency, items.length);
  await Promise.all(Array.from({ length: workerCount }, () => worker()));
  return results;
}

async function rewriteCopiedHtmlLinks(outDir, htmlRelativePaths) {
  const rewrittenHtmlByPath = new Map();

  await mapWithConcurrency(htmlRelativePaths, async (relativeHtmlPath) => {
    const htmlFilePath = path.join(outDir, ...relativeHtmlPath.split('/'));
    const htmlContent = await fs.readFile(htmlFilePath, 'utf8');
    const rewrittenHtml = rewriteAbsoluteUrlsToRelative(htmlContent, relativeHtmlPath);

    rewrittenHtmlByPath.set(relativeHtmlPath, rewrittenHtml);

    if (rewrittenHtml !== htmlContent) {
      await fs.writeFile(htmlFilePath, rewrittenHtml, 'utf8');
    }
  });

  return rewrittenHtmlByPath;
}

function toDirectoryAliasPath(relativeHtmlPath) {
  if (!relativeHtmlPath.endsWith('.html')) {
    return null;
  }

  if (path.posix.basename(relativeHtmlPath) === 'index.html') {
    return null;
  }

  const withoutExtension = relativeHtmlPath.slice(0, -'.html'.length);
  return `${withoutExtension}/index.html`;
}

async function createDirectoryRouteAliases(outDir, htmlRelativePaths, rewrittenHtmlByPath) {
  const aliasEntries = htmlRelativePaths
    .map((relativeHtmlPath) => {
      const aliasPath = toDirectoryAliasPath(relativeHtmlPath);
      if (aliasPath === null) {
        return null;
      }

      return { aliasPath, sourcePath: relativeHtmlPath };
    })
    .filter((entry) => entry !== null);

  await mapWithConcurrency(aliasEntries, async (entry) => {
    const sourceHtmlContent = rewrittenHtmlByPath.get(entry.sourcePath);
    if (sourceHtmlContent === undefined) {
      return;
    }

    const aliasAbsolutePath = path.join(outDir, ...entry.aliasPath.split('/'));
    const rebasedHtmlContent = rebaseRelativeUrls(sourceHtmlContent, entry.sourcePath, entry.aliasPath);

    await fs.mkdir(path.dirname(aliasAbsolutePath), { recursive: true });
    await fs.writeFile(aliasAbsolutePath, rebasedHtmlContent, 'utf8');
  });
}

async function rewriteAndAliasHtml(outDir) {
  const htmlFiles = await listHtmlFiles(outDir);
  const htmlRelativePaths = htmlFiles.map((htmlFile) => toPosixPath(path.relative(outDir, htmlFile)));
  const rewrittenHtmlByPath = await rewriteCopiedHtmlLinks(outDir, htmlRelativePaths);

  await createDirectoryRouteAliases(outDir, htmlRelativePaths, rewrittenHtmlByPath);
}

async function main() {
  const target = process.argv[2];
  const distDir = path.join(__dirname, '../dist');
  let outDir;

  if (target === 'prod') {
    outDir = path.join(__dirname, '../../../src/client/resources/assets/myqolscripts/pages');
  } else if (target === 'dev') {
    outDir = path.join(__dirname, '../../out');
  } else {
    console.error('Please specify a target: "prod" or "dev"');
    process.exit(1);
  }

  console.log(`[MQS Build] Preparing to copy files to ${outDir}...`);

  try {
    await fs.access(distDir);
    await fs.rm(outDir, { recursive: true, force: true });
    await fs.mkdir(outDir, { recursive: true });
    await fs.cp(distDir, outDir, { recursive: true });
    await rewriteAndAliasHtml(outDir);

    console.log(`[MQS Build] Successfully copied dist/ to ${target} directory, rewrote absolute links, and generated directory route aliases.`);
  } catch (err) {
    console.error(`[MQS Build] Failed to copy files: ${err.message}`);
    process.exit(1);
  }
}

main();
