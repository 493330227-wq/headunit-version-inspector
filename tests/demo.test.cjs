const {chromium}=require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const fs=require('fs');const path=require('path');const assert=require('assert');
(async()=>{
 const root=path.resolve(__dirname,'..');
 const browser=await chromium.launch({headless:true,...(process.env.CHROME_BIN ? {executablePath:process.env.CHROME_BIN} : {channel:'chrome'})});
 const ctx=await browser.newContext({viewport:{width:1100,height:850},deviceScaleFactor:1});
 const p=await ctx.newPage();const errors=[];p.on('pageerror',e=>errors.push(e.message));
 await p.goto('file://'+root+'/演示页面.html');
 const fixtures=JSON.parse(fs.readFileSync(root+'/demo-fixtures.json'));let checks=0;
 async function ok(condition,msg){assert(condition,msg);checks++;}
 for(let i=0;i<fixtures.length;i++){
  const f=fixtures[i];await p.selectOption('#scenario',String(i));
  await ok(await p.locator('#result-page').isVisible(),'result default '+i);
  await ok(await p.locator('#declared').innerText()==='Android '+f.release,'declared '+i);
  await ok(await p.locator('#inferred').innerText()===f.inferred,'inferred '+i);
  await p.click('#next');await ok(await p.locator('#recommend-page').isVisible(),'recommend page '+i);
  await ok(await p.locator('#recommend-title').innerText()===f.title,'recommendation '+i);
  if(f.source){await ok(await p.locator('#download').getAttribute('href')===f.source,'source '+i)}
  else {await ok(!await p.locator('#download').isVisible(),'hold source '+i);await ok(await p.locator('#download').getAttribute('href')===null,'no stale source '+i);}
  await ok(await p.locator('#alternative-card').isVisible()===!!f.alternative.source,'fallback visibility '+i);
  await ok(await p.locator('#alternative-download').getAttribute('href')===(f.alternative.source||null),'fallback link '+i);
  await p.click('#back');await ok(await p.locator('#result-page').isVisible(),'back '+i);
 }
 await p.selectOption('#scenario','0');await p.click('#details');await ok(await p.locator('#evidence').isVisible(),'details dialog');await p.click('#close-evidence');
 for(const width of [320,600,1024]){
  await p.setViewportSize({width,height:800});
  for(const name of ['result','recommend']){
   await p.click('#'+name+'-tab');
   await ok(await p.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'no overflow '+width+' '+name);
   await p.screenshot({path:root+'/演示-'+name+'-'+width+'.png',fullPage:true});
  }
 }
 await p.setViewportSize({width:1100,height:850});await p.click('#recommend-tab');
 await ctx.route('https://github.com/**',route=>route.fulfill({status:200,contentType:'text/html',body:'Download page navigation test'}));
 const pop=ctx.waitForEvent('page');await p.click('#download');const target=await pop;await target.waitForLoadState();
 await ok(target.url()===fixtures[0].source,'download opens expected page');await target.close();
 await ok(errors.length===0,'no JavaScript errors');
 await browser.close();const result={status:'PASS',checks,scenarios:fixtures.length,widths:[320,600,1024],notes:'Browser demo only. GitHub click tested with intercepted navigation; actual release URLs separately verified.'};
 fs.writeFileSync(root+'/演示验证.json',JSON.stringify(result,null,2));console.log(result);
})().catch(e=>{console.error(e);process.exit(1)});
