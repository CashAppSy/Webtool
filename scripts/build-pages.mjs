import fs from 'node:fs';
import {build} from 'esbuild';
await import('./build.mjs');
fs.rmSync('pages/dist',{recursive:true,force:true});
fs.cpSync('dist','pages/dist',{recursive:true});
await build({entryPoints:['server/pages.mjs'],bundle:true,format:'esm',platform:'browser',target:'es2022',outfile:'pages/dist/_worker.js',minify:true});
fs.writeFileSync('pages/dist/_routes.json',JSON.stringify({version:1,include:['/api/*','/admin','/admin/','/admin.html'],exclude:[]}));
console.log('Pages bundle ready in pages/dist. Scheduled delivery stays on the existing Worker with the same D1 database.');
