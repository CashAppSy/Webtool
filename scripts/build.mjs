import {build as viteBuild} from 'vite';
import {build as esbuild} from 'esbuild';
import fs from 'node:fs';
await viteBuild({build:{rollupOptions:{input:{main:'index.html',admin:'admin.html'}}}});
await import('./seed.mjs');
if(process.env.BUILD_DEMO==='true')fs.copyFileSync('.runtime/seed.json','dist/seed.json');
fs.cpSync('.runtime/media','dist/media',{recursive:true});
for(const path of ['css','MTN_Logo.png','manifest.webmanifest','_headers']){fs.cpSync(path,`dist/${path}`,{recursive:true});}
// Legacy data stays available for offline/static fallback. All CMS data comes from the API.
await esbuild({entryPoints:['js/firebase-sw.js'],bundle:true,outfile:'dist/sw.js',format:'iife',minify:true});
