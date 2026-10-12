import {build as viteBuild} from 'vite';
import {build as esbuild} from 'esbuild';
import fs from 'node:fs';
import crypto from 'node:crypto';
await viteBuild({build:{rollupOptions:{input:{main:'index.html',admin:'admin.html'}}}});
await import('./seed.mjs');
if(process.env.BUILD_DEMO==='true')fs.copyFileSync('.runtime/seed.json','dist/seed.json');
fs.cpSync('.runtime/media','dist/media',{recursive:true});
for(const path of ['css','training-hub.png','MTN_Logo.png','manifest.webmanifest','_headers']){fs.cpSync(path,`dist/${path}`,{recursive:true});}
// Legacy data stays available for offline/static fallback. All CMS data comes from the API.
const shell=['/','/index.html','/manifest.webmanifest','/training-hub.png',...fs.readdirSync('dist/assets').filter(name=>!name.startsWith('admin-')).map(name=>'/assets/'+name)];
fs.writeFileSync('dist/offline-shell.json',JSON.stringify(shell));
const shellVersion=crypto.createHash('sha256').update(JSON.stringify(shell)).digest('hex').slice(0,12);
await esbuild({define:{__SHELL_VERSION__:JSON.stringify(shellVersion)},entryPoints:['js/firebase-sw.js'],bundle:true,outfile:'dist/sw.js',format:'iife',minify:true});
