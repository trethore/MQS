// @ts-check

import tailwindcss from "@tailwindcss/vite";
import { defineConfig } from "astro/config";

import react from "@astrojs/react";

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
        "@": new URL("./src", import.meta.url).pathname,
      },
    },
  },

  integrations: [react()],
});
