// Generate disposable development credentials only in the official Firebase Auth emulator.
import fs from 'node:fs';import crypto from 'node:crypto';
const origin='http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/accounts:';
const password=crypto.randomBytes(24).toString('base64url'),email='admin@example.test';
const created=await fetch(origin+'signUp?key=demo-key',{method:'POST',headers:{'Content-Type':'application/json',Authorization:'Bearer owner'},body:JSON.stringify({email,password,localId:'onboarding-admin'})});
if(!created.ok){const e=await created.json();throw Error(e.error?.message||'Emulator user creation failed. Restart the disposable Auth emulator to clear existing users.');}
const response=await fetch(origin+'signInWithPassword?key=demo-key',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({email,password,returnSecureToken:true})});const auth=await response.json();if(!response.ok)throw Error('Emulator sign-in failed');
fs.mkdirSync('.runtime',{recursive:true});fs.writeFileSync('.runtime/test-auth.json',JSON.stringify({email,password,uid:auth.localId,idToken:auth.idToken}),{mode:0o600});console.log('Created disposable local admin. Credentials are in ignored .runtime/test-auth.json; never use these in production.');
