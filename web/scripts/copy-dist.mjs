import path from 'node:path';
import { cp, mkdir, rm, stat } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const webDir = path.resolve(__dirname, '..');
const repositoryRoot = path.resolve(webDir, '..');
const distDir = path.join(webDir, 'dist');

const targetDirectories = {
  dev: path.join(webDir, 'out'),
  prod: path.join(repositoryRoot, 'src', 'client', 'resources', 'assets', 'myqolscripts', 'web'),
};

async function main() {
  const mode = process.argv[2];
  const targetDirectory = targetDirectories[mode];

  if (!targetDirectory) {
    throw new Error('Expected mode to be one of: dev, prod');
  }

  await assertDirectoryExists(distDir, 'dist');
  await resetDirectory(targetDirectory);
  await cp(distDir, targetDirectory, { recursive: true });

  process.stdout.write(`Copied ${distDir} to ${targetDirectory}\n`);
}

async function assertDirectoryExists(directoryPath, directoryName) {
  let directoryStats;

  try {
    directoryStats = await stat(directoryPath);
  } catch (error) {
    if (error && typeof error === 'object' && 'code' in error && error.code === 'ENOENT') {
      throw new Error(
        `Missing ${directoryName} directory at ${directoryPath}. Run the Vite build first.`
      );
    }

    throw error;
  }

  if (!directoryStats.isDirectory()) {
    throw new Error(`Expected ${directoryPath} to be a directory.`);
  }
}

async function resetDirectory(directoryPath) {
  await rm(directoryPath, { recursive: true, force: true });
  await mkdir(directoryPath, { recursive: true });
}

try {
  await main();
} catch (error) {
  const message = error instanceof Error ? error.message : String(error);
  process.stderr.write(`${message}\n`);
  process.exitCode = 1;
}
