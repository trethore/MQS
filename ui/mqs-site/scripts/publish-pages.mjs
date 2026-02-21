import {
    cpSync,
    existsSync,
    mkdirSync,
    readdirSync,
    readFileSync,
    rmSync,
    writeFileSync,
} from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(scriptDir, "..");
const distDir = path.join(projectRoot, "dist");
const publishTarget = process.argv[2] ?? "prod";

if (publishTarget !== "prod" && publishTarget !== "dev") {
    throw new Error(`Unknown publish target '${publishTarget}'. Use 'prod' or 'dev'.`);
}

const pagesDir = publishTarget === "dev"
    ? path.resolve(projectRoot, "..", "out")
    : path.resolve(
        projectRoot,
        "..",
        "..",
        "src",
        "client",
        "resources",
        "assets",
        "myqolscripts",
        "pages",
    );

if (!existsSync(distDir)) {
    throw new Error("Astro dist directory not found. Run astro build first.");
}

rmSync(pagesDir, { recursive: true, force: true });
mkdirSync(pagesDir, { recursive: true });
cpSync(distDir, pagesDir, { recursive: true, force: true });

rewriteHtmlLinks(pagesDir);

console.log(`Published UI pages (${publishTarget}) to ${pagesDir}`);

function rewriteHtmlLinks(rootDir) {
    const htmlFiles = collectHtmlFiles(rootDir);
    for (const htmlFilePath of htmlFiles) {
        const directoryRelativePath = path.relative(rootDir, path.dirname(htmlFilePath));
        const levelCount = directoryRelativePath === "" ? 0 : directoryRelativePath.split(path.sep).length;
        const prefix = levelCount === 0 ? "./" : "../".repeat(levelCount);

        const htmlContent = readFileSync(htmlFilePath, "utf8");
        const rewrittenContent = htmlContent.replace(
            /(\b(?:href|src|component-url|renderer-url|before-hydration-url)=["'])\/([^"']*)(["'])/g,
            (fullMatch, attributeStart, rawPath, attributeEnd) => {
                const normalizedPath = normalizeAbsolutePath(rawPath);
                return `${attributeStart}${prefix}${normalizedPath}${attributeEnd}`;
            },
        );

        writeFileSync(htmlFilePath, rewrittenContent, "utf8");
    }
}

function collectHtmlFiles(directoryPath) {
    const htmlFiles = [];
    const directoryEntries = readdirSync(directoryPath, { withFileTypes: true });

    for (const directoryEntry of directoryEntries) {
        const entryPath = path.join(directoryPath, directoryEntry.name);
        if (directoryEntry.isDirectory()) {
            htmlFiles.push(...collectHtmlFiles(entryPath));
            continue;
        }

        if (directoryEntry.isFile() && directoryEntry.name.endsWith(".html")) {
            htmlFiles.push(entryPath);
        }
    }

    return htmlFiles;
}

function normalizeAbsolutePath(rawPath) {
    if (rawPath === "") {
        return "index.html";
    }

    const suffixMatch = rawPath.match(/[?#].*$/);
    const suffix = suffixMatch ? suffixMatch[0] : "";
    const pathWithoutSuffix = suffixMatch ? rawPath.slice(0, -suffix.length) : rawPath;

    if (pathWithoutSuffix === "") {
        return `index.html${suffix}`;
    }

    if (path.posix.extname(pathWithoutSuffix) === "") {
        if (pathWithoutSuffix.endsWith("/")) {
            return `${pathWithoutSuffix}index.html${suffix}`;
        }

        return `${pathWithoutSuffix}/index.html${suffix}`;
    }

    return `${pathWithoutSuffix}${suffix}`;
}
