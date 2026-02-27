// @ts-check

import tailwindcss from "@tailwindcss/vite";
import { defineConfig } from "astro/config";

import react from "@astrojs/react";

/**
 * @param {{ pathname: string }} fileUrl
 */
function toSafeFilePath(fileUrl) {
  const decodedPathname = decodeURIComponent(fileUrl.pathname);
  return decodedPathname.replace(/^\/([A-Za-z]:\/)/, "$1");
}

export default defineConfig({
  build: {
    assetsPrefix: ".",
    format: "file",
  },

  vite: {
    // @ts-expect-error - Version mismatch between @tailwindcss/vite and astro vite types
    plugins: [tailwindcss()],
    resolve: {
      alias: {
        "@": toSafeFilePath(new URL("./src", import.meta.url)),
      },
    },
  },

  integrations: [react()],
});
