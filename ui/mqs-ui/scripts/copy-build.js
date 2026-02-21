import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

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

    console.log(`[MQS Build] Successfully copied dist/ to ${target} directory.`);
  } catch (err) {
    console.error(`[MQS Build] Failed to copy files: ${err.message}`);
    process.exit(1);
  }
}

main();
