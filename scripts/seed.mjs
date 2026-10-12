import fs from 'node:fs';
import vm from 'node:vm';
import {createHash} from 'node:crypto';
import { splitPayload } from '../server/content.mjs';
const index=fs.readFileSync('data/services/index.js','utf8');
const ctx={document:{getElementsByTagName:()=>[],write:()=>{}}};
vm.runInNewContext(index,ctx);
for(const file of ctx.SERVICE_FILES) vm.runInNewContext(fs.readFileSync(`data/services/${file}`,'utf8'),ctx,{filename:file});
const categories=JSON.parse(JSON.stringify(ctx.CATEGORIES));
for(const s of ctx.SERVICES_DATA){for(const id of (s.categories||[s.category]))if(!categories.some(c=>c.id===id))categories.push({id,title_ar:id,title_en:id,icon:'📁'});}
const categoryNames={Prepaid:['مسبق الدفع','Prepaid'],Postpaid:['لاحق الدفع','Postpaid'],Entertement:['الترفيه','Entertainment'],Bundles:['الباقات','Bundles'],'Cash Mobile':['الدفع والتحويل','Cash Mobile'],Adsl:['إنترنت ADSL','ADSL'],Services:['خدمات الخطوط','Line services'],Rooming:['التجوال','Roaming'],'S-Services':['خدمات إضافية','Additional services'],'c-lines':['الخطوط والاشتراكات','Lines & subscriptions'],'c-promo':['العروض','Offers'],'c-number':['الأرقام والشرائح','Numbers & SIM'],'c-security':['الأمان والبطاقة','Security & SIM']};
for(const c of categories){if(categoryNames[c.id]){[c.title_ar,c.title_en]=categoryNames[c.id];}}
const content={services:JSON.parse(JSON.stringify(ctx.SERVICES_DATA)).map((s,i)=>({...s,status:'published',order:i,blocks:[]})),categories:categories.map((c,i)=>({...c,order:i,enabled:true})),sections:[],notifications:[],settings:{id:'site',title_ar:'Training Hub',title_en:'Training Hub',subtitle_ar:'كل ما تحتاجه، في مكان واحد. اكتشف الخدمات وتعلّم بخطوات واضحة.',subtitle_en:'Everything you need, in one place. Explore services and learn step by step.',footer_ar:'Training Hub · منصة المعرفة والتدريب',footer_en:'Training Hub · Knowledge & training',heroEnabled:true}};
fs.mkdirSync('.runtime/media',{recursive:true});
function extractImages(value){if(!value||typeof value!=='object')return;for(const [key,item]of Object.entries(value)){if(typeof item==='string'&&/^data:image\/(png|jpeg|webp|gif);base64,/.test(item)){const [header,base64]=item.split(',');const bytes=Buffer.from(base64,'base64');const ext=header.match(/image\/(\w+)/)[1];const name=createHash('sha256').update(bytes).digest('hex')+'.'+ext;fs.writeFileSync('.runtime/media/'+name,bytes);value[key]='/media/'+name;}else extractImages(item);}}
extractImages(content);
const quote=s=>"'"+String(s).replaceAll("'","''")+"'";
let sql='INSERT OR IGNORE INTO seed_registry(kind,id) SELECT DISTINCT kind,id FROM entities;\n';
for(const kind of ['services','categories','sections','notifications','settings'])for(const entity of (kind==='settings'?[content.settings]:content[kind])){splitPayload(entity,16000).forEach((payload,position)=>{sql+=`INSERT INTO entities(kind,id,position,payload) SELECT ${quote(kind)},${quote(entity.id)},${position},${quote(payload)} WHERE NOT EXISTS(SELECT 1 FROM seed_registry WHERE kind=${quote(kind)} AND id=${quote(entity.id)});\n`;});sql+=`INSERT OR IGNORE INTO seed_registry(kind,id) VALUES(${quote(kind)},${quote(entity.id)});\n`;}
fs.mkdirSync('.runtime',{recursive:true});fs.writeFileSync('.runtime/seed.sql',sql);fs.writeFileSync('.runtime/seed.json',JSON.stringify(content));console.log(`Prepared ${content.services.length} services and ${content.categories.length} categories; existing entries are preserved.`);
