import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

function hasFileExtension(pathname) {
  const lastSegment = pathname.split('/').pop() ?? '';
  return lastSegment.includes('.');
}

function normalizeLinkPath(pathname, attribute) {
  const normalized = path.posix.normalize(pathname).replace(/^\/+/, '');
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
  const [pathname, suffix = ''] = urlPath.match(/^([^?#]*)([?#].*)?$/)?.slice(1) ?? [urlPath, ''];
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
  return htmlContent.replace(/\b(href|src)=(['"])\/(?!\/)([^'"]*)\2/g, (match, attribute, quote, urlPath) => {
    const rewritten = absolutishUrlToRelative(urlPath, attribute, relativeHtmlPath);
    return `${attribute}=${quote}${rewritten}${quote}`;
  });
}

function shouldLeaveUrlUnchanged(urlPath) {
  return (
    urlPath.startsWith('/') ||
    urlPath.startsWith('#') ||
    urlPath.startsWith('http://') ||
    urlPath.startsWith('https://') ||
    urlPath.startsWith('//') ||
    urlPath.startsWith('mailto:') ||
    urlPath.startsWith('tel:') ||
    urlPath.startsWith('data:') ||
    urlPath.startsWith('javascript:')
  );
}

function rebaseRelativeUrls(htmlContent, sourceRelativeHtmlPath, destinationRelativeHtmlPath) {
  return htmlContent.replace(/\b(href|src|component-url|renderer-url|before-hydration-url)=(['"])([^'"]*)\2/g, (match, attribute, quote, urlPath) => {
    if (shouldLeaveUrlUnchanged(urlPath)) {
      return match;
    }

    const [pathname, suffix = ''] = urlPath.match(/^([^?#]*)([?#].*)?$/)?.slice(1) ?? [urlPath, ''];
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
  const entries = await fs.readdir(directory, { withFileTypes: true });
  const htmlFiles = [];

  for (const entry of entries) {
    const fullPath = path.join(directory, entry.name);

    if (entry.isDirectory()) {
      const nestedHtmlFiles = await listHtmlFiles(fullPath);
      htmlFiles.push(...nestedHtmlFiles);
      continue;
    }

    if (entry.isFile() && entry.name.endsWith('.html')) {
      htmlFiles.push(fullPath);
    }
  }

  return htmlFiles;
}

async function rewriteCopiedHtmlLinks(outDir) {
  const htmlFiles = await listHtmlFiles(outDir);

  for (const htmlFile of htmlFiles) {
    const relativeHtmlPath = path.relative(outDir, htmlFile).split(path.sep).join('/');
    const htmlContent = await fs.readFile(htmlFile, 'utf8');
    const rewrittenHtml = rewriteAbsoluteUrlsToRelative(htmlContent, relativeHtmlPath);

    if (rewrittenHtml !== htmlContent) {
      await fs.writeFile(htmlFile, rewrittenHtml, 'utf8');
    }
  }
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

async function createDirectoryRouteAliases(outDir) {
  const htmlFiles = await listHtmlFiles(outDir);

  for (const htmlFile of htmlFiles) {
    const relativeHtmlPath = path.relative(outDir, htmlFile).split(path.sep).join('/');
    const aliasPath = toDirectoryAliasPath(relativeHtmlPath);

    if (aliasPath === null) {
      continue;
    }

    const aliasAbsolutePath = path.join(outDir, ...aliasPath.split('/'));
    const sourceHtmlContent = await fs.readFile(htmlFile, 'utf8');
    const rebasedHtmlContent = rebaseRelativeUrls(sourceHtmlContent, relativeHtmlPath, aliasPath);

    await fs.mkdir(path.dirname(aliasAbsolutePath), { recursive: true });
    await fs.writeFile(aliasAbsolutePath, rebasedHtmlContent, 'utf8');
  }
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
    await fs.rm(outDir, { recursive: true, force: true });
    await fs.mkdir(outDir, { recursive: true });
    await fs.cp(distDir, outDir, { recursive: true });
    await rewriteCopiedHtmlLinks(outDir);
    await createDirectoryRouteAliases(outDir);

    console.log(`[MQS Build] Successfully copied dist/ to ${target} directory, rewrote absolute links, and generated directory route aliases.`);
  } catch (err) {
    console.error(`[MQS Build] Failed to copy files: ${err.message}`);
    process.exit(1);
  }
}

main();
