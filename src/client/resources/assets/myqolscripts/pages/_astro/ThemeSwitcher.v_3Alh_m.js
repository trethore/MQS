import{j as c,S as x,b as u,d as w}from"./utils.BeYzBkeC.js";import{r as i}from"./index.DwQS_Y10.js";/**
 * @license lucide-react v0.575.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const f=(...e)=>e.filter((t,s,n)=>!!t&&t.trim()!==""&&n.indexOf(t)===s).join(" ").trim();/**
 * @license lucide-react v0.575.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const C=e=>e.replace(/([a-z0-9])([A-Z])/g,"$1-$2").toLowerCase();/**
 * @license lucide-react v0.575.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const E=e=>e.replace(/^([A-Z])|[\s-_]+(\w)/g,(t,s,n)=>n?n.toUpperCase():s.toLowerCase());/**
 * @license lucide-react v0.575.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const g=e=>{const t=E(e);return t.charAt(0).toUpperCase()+t.slice(1)};/**
 * @license lucide-react v0.575.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */var T={xmlns:"http://www.w3.org/2000/svg",width:24,height:24,viewBox:"0 0 24 24",fill:"none",stroke:"currentColor",strokeWidth:2,strokeLinecap:"round",strokeLinejoin:"round"};/**
 * @license lucide-react v0.575.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const z=e=>{for(const t in e)if(t.startsWith("aria-")||t==="role"||t==="title")return!0;return!1};/**
 * @license lucide-react v0.575.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const j=i.forwardRef(({color:e="currentColor",size:t=24,strokeWidth:s=2,absoluteStrokeWidth:n,className:d="",children:o,iconNode:h,...l},r)=>i.createElement("svg",{ref:r,...T,width:t,height:t,stroke:e,strokeWidth:n?Number(s)*24/Number(t):s,className:f("lucide",d),...!o&&!z(l)&&{"aria-hidden":"true"},...l},[...h.map(([a,m])=>i.createElement(a,m)),...Array.isArray(o)?o:[o]]));/**
 * @license lucide-react v0.575.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const k=(e,t)=>{const s=i.forwardRef(({className:n,...d},o)=>i.createElement(j,{ref:o,iconNode:t,className:f(`lucide-${C(g(e))}`,`lucide-${e}`,n),...d}));return s.displayName=g(e),s};/**
 * @license lucide-react v0.575.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const S=[["path",{d:"M20.985 12.486a9 9 0 1 1-9.473-9.472c.405-.022.617.46.402.803a6 6 0 0 0 8.268 8.268c.344-.215.825-.004.803.401",key:"kfwtm"}]],L=k("moon",S);/**
 * @license lucide-react v0.575.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const N=[["circle",{cx:"12",cy:"12",r:"4",key:"4exip2"}],["path",{d:"M12 2v2",key:"tus03m"}],["path",{d:"M12 20v2",key:"1lh1kg"}],["path",{d:"m4.93 4.93 1.41 1.41",key:"149t6j"}],["path",{d:"m17.66 17.66 1.41 1.41",key:"ptbguv"}],["path",{d:"M2 12h2",key:"1t8f8n"}],["path",{d:"M20 12h2",key:"1q8mjw"}],["path",{d:"m6.34 17.66-1.41 1.41",key:"1m8zz5"}],["path",{d:"m19.07 4.93-1.41 1.41",key:"1shlcs"}]],_=k("sun",N),q=w("inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-md text-sm font-medium transition-all disabled:pointer-events-none disabled:opacity-50 [&_svg]:pointer-events-none [&_svg:not([class*='size-'])]:size-4 shrink-0 [&_svg]:shrink-0 outline-none focus-visible:border-ring focus-visible:ring-ring/50 focus-visible:ring-[3px] aria-invalid:ring-destructive/20 dark:aria-invalid:ring-destructive/40 aria-invalid:border-destructive",{variants:{variant:{default:"bg-primary text-primary-foreground hover:bg-primary/90",destructive:"bg-destructive text-white hover:bg-destructive/90 focus-visible:ring-destructive/20 dark:focus-visible:ring-destructive/40 dark:bg-destructive/60",outline:"border bg-background shadow-xs hover:bg-accent hover:text-accent-foreground dark:bg-input/30 dark:border-input dark:hover:bg-input/50",secondary:"bg-secondary text-secondary-foreground hover:bg-secondary/80",ghost:"hover:bg-accent hover:text-accent-foreground dark:hover:bg-accent/50",link:"text-primary underline-offset-4 hover:underline"},size:{default:"h-9 px-4 py-2 has-[>svg]:px-3",xs:"h-6 gap-1 rounded-md px-2 text-xs has-[>svg]:px-1.5 [&_svg:not([class*='size-'])]:size-3",sm:"h-8 rounded-md gap-1.5 px-3 has-[>svg]:px-2.5",lg:"h-10 rounded-md px-6 has-[>svg]:px-4",icon:"size-9","icon-xs":"size-6 rounded-md [&_svg:not([class*='size-'])]:size-3","icon-sm":"size-8","icon-lg":"size-10"}},defaultVariants:{variant:"default",size:"default"}});function R({className:e,variant:t="default",size:s="default",asChild:n=!1,...d}){const o=n?x:"button";return c.jsx(o,{"data-slot":"button","data-variant":t,"data-size":s,className:u(q({variant:t,size:s,className:e})),...d})}const y="mqs-theme";function A(e){return e==="light"||e==="dark"||e==="system"}function b(){return window.matchMedia("(prefers-color-scheme: dark)").matches?"dark":"light"}function v(e){return e==="system"?b():e}function p(e){document.documentElement.classList.toggle("dark",e==="dark")}function M(){try{const e=localStorage.getItem(y);return A(e)?e:"system"}catch{return"system"}}function $(e){try{localStorage.setItem(y,e)}catch{}}function B(){const[e,t]=i.useState(!1),[s,n]=i.useState("system"),[d,o]=i.useState("dark"),h=i.useRef(null);i.useEffect(()=>{const r=M(),a=v(r);n(r),o(a),p(a)},[]),i.useEffect(()=>{const r=window.matchMedia("(prefers-color-scheme: dark)"),a=()=>{if(s!=="system")return;const m=b();o(m),p(m)};return r.addEventListener("change",a),()=>r.removeEventListener("change",a)},[s]),i.useEffect(()=>{if(!e)return;const r=m=>{h.current&&!h.current.contains(m.target)&&t(!1)},a=m=>{m.key==="Escape"&&t(!1)};return document.addEventListener("mousedown",r),document.addEventListener("keydown",a),()=>{document.removeEventListener("mousedown",r),document.removeEventListener("keydown",a)}},[e]);const l=r=>{const a=v(r);n(r),o(a),p(a),$(r),t(!1)};return c.jsxs("div",{ref:h,className:"relative shrink-0",children:[c.jsxs(R,{type:"button",variant:"ghost",size:"icon-sm",className:"mqs-nav-link","aria-label":"Theme options","aria-haspopup":"menu","aria-expanded":e,onClick:()=>t(r=>!r),children:[c.jsx(_,{className:u("size-4",d==="dark"?"hidden":"block")}),c.jsx(L,{className:u("size-4",d==="light"?"hidden":"block")})]}),e&&c.jsxs("div",{className:"mqs-theme-menu absolute right-0 top-[calc(100%+0.25rem)] z-30 min-w-36 rounded-xl p-1",role:"menu","aria-label":"Theme",children:[c.jsx("button",{type:"button",className:u("mqs-theme-item",s==="light"&&"mqs-theme-item-active"),onClick:()=>l("light"),role:"menuitemradio","aria-checked":s==="light",children:"Light"}),c.jsx("button",{type:"button",className:u("mqs-theme-item",s==="dark"&&"mqs-theme-item-active"),onClick:()=>l("dark"),role:"menuitemradio","aria-checked":s==="dark",children:"Dark"}),c.jsx("button",{type:"button",className:u("mqs-theme-item",s==="system"&&"mqs-theme-item-active"),onClick:()=>l("system"),role:"menuitemradio","aria-checked":s==="system",children:"System"})]})]})}export{B as ThemeSwitcher};
