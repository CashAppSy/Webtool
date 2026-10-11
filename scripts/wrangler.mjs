import {spawn} from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
const runtime=path.resolve('.runtime');fs.mkdirSync(runtime,{recursive:true});
const child=spawn(process.execPath,['node_modules/wrangler/bin/wrangler.js',...process.argv.slice(2)],{stdio:'inherit',env:{...process.env,XDG_CONFIG_HOME:process.env.XDG_CONFIG_HOME||path.join(runtime,'config'),WRANGLER_LOG_PATH:path.join(runtime,'wrangler.log'),WRANGLER_SEND_METRICS:'false'}});
for(const signal of ['SIGINT','SIGTERM'])process.on(signal,()=>child.kill(signal));
child.on('error',error=>{console.error(error.message);process.exitCode=1;});child.on('exit',(code,signal)=>{process.exitCode=code??(signal==='SIGINT'?130:1);});
