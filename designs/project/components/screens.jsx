// Screens — themed via CSS variables + Import submenu + Theme picker

const { useState: useState2 } = React;
const cv = (n) => `var(--${n})`;

function ScreenHeader({ title, onBack, action }) {
  return (
    <div style={{ padding: '14px 8px 14px 12px', display: 'flex', alignItems: 'center', gap: 8, background: cv('bg-elev'), borderBottom: '1px solid ' + cv('border') }}>
      <button onClick={onBack} style={{ width: 40, height: 40, borderRadius: 12, border: 'none', background: cv('chip'), cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <IconBack stroke={cv('text')} size={18} sw={2.2}/>
      </button>
      <div style={{ flex: 1, fontSize: 18, fontWeight: 700, color: cv('text'), letterSpacing: -0.2 }}>{title}</div>
      {action}
    </div>
  );
}

function SideMenu({ onClose, onNavigate, accent }) {
  const [importOpen, setImportOpen] = useState2(false);
  const goalPct = 75;
  const Item = ({ icon, label, onClick, chev }) => (
    <button onClick={onClick} style={{ width: '100%', background: 'transparent', border: 'none', cursor: 'pointer', padding: '12px 8px', display: 'flex', alignItems: 'center', gap: 14, borderRadius: 10, textAlign: 'left' }}>
      <div style={{ width: 36, height: 36, borderRadius: 10, background: cv('chip'), display: 'flex', alignItems: 'center', justifyContent: 'center' }}>{icon}</div>
      <div style={{ flex: 1, fontSize: 15, color: cv('text'), fontWeight: 500 }}>{label}</div>
      {chev && <IconChev size={16} stroke={cv('text-4')}/>}
    </button>
  );
  const SubItem = ({ label, onClick }) => (
    <button onClick={onClick} style={{ width: '100%', background: 'transparent', border: 'none', cursor: 'pointer', padding: '10px 8px 10px 58px', display: 'flex', alignItems: 'center', borderRadius: 10, textAlign: 'left', fontSize: 14, color: cv('text-2'), fontWeight: 500 }}>
      {label}
    </button>
  );
  const Section = ({ children }) => (
    <div style={{ fontSize: 10, letterSpacing: 1.2, color: accent, fontWeight: 700, textTransform: 'uppercase', padding: '16px 8px 4px' }}>{children}</div>
  );
  return (
    <div style={{ position: 'absolute', inset: 0, zIndex: 50, display: 'flex' }}>
      <div style={{ width: 300, background: cv('bg-elev'), height: '100%', display: 'flex', flexDirection: 'column', boxShadow: '10px 0 30px rgba(0,0,0,0.25)' }}>
        <div style={{ padding: '22px 20px 18px', borderBottom: '1px solid ' + cv('divider') }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 14 }}>
            <div style={{ width: 40, height: 40, borderRadius: 12, background: `linear-gradient(135deg, ${accent} 0%, ${accent}aa 100%)`, display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', fontWeight: 800, fontSize: 16 }}>U</div>
            <button onClick={onClose} style={{ width: 32, height: 32, borderRadius: 10, border: 'none', background: cv('chip'), cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <IconClose size={16} stroke={cv('text-3')}/>
            </button>
          </div>
          <div style={{ fontSize: 11, letterSpacing: 1.2, color: accent, fontWeight: 700, textTransform: 'uppercase' }}>Goal 2027</div>
          <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', marginTop: 4 }}>
            <div style={{ fontSize: 18, fontWeight: 700, color: cv('text') }}>₹1 Crore target</div>
            <div style={{ fontSize: 14, fontWeight: 700, color: accent }}>{goalPct}%</div>
          </div>
          <div style={{ marginTop: 8, height: 6, background: cv('divider'), borderRadius: 3, overflow: 'hidden' }}>
            <div style={{ width: goalPct + '%', height: '100%', background: accent, borderRadius: 3 }} />
          </div>
          <div style={{ fontSize: 11, color: cv('text-4'), marginTop: 6 }}>₹24.5 L to go · 8 months left</div>
        </div>
        <div style={{ padding: '8px 12px', flex: 1, overflowY: 'auto' }}>
          <Section>Overview</Section>
          <Item icon={<IconGrid size={18} stroke={cv('text-2')}/>} label="Month overview" onClick={() => onNavigate('overview')} />
          <Section>Analytics</Section>
          <Item icon={<IconLine size={18} stroke={cv('text-2')}/>}  label="Graph"        onClick={() => onNavigate('graph')} />
          <Item icon={<IconPie size={18} stroke={cv('text-2')}/>}   label="Allocation"   onClick={() => onNavigate('allocation')} />
          <Item icon={<IconTrend size={18} stroke={cv('text-2')}/>} label="Asset Trends" onClick={() => onNavigate('trends')} />
          <Section>Data Management</Section>
          <Item icon={<IconUp size={18} stroke={cv('text-2')}/>}   label="Export" onClick={() => {}} />
          <button onClick={() => setImportOpen(!importOpen)} style={{ width: '100%', background: 'transparent', border: 'none', cursor: 'pointer', padding: '12px 8px', display: 'flex', alignItems: 'center', gap: 14, borderRadius: 10, textAlign: 'left' }}>
            <div style={{ width: 36, height: 36, borderRadius: 10, background: cv('chip'), display: 'flex', alignItems: 'center', justifyContent: 'center' }}><IconDown size={18} stroke={cv('text-2')}/></div>
            <div style={{ flex: 1, fontSize: 15, color: cv('text'), fontWeight: 500 }}>Import</div>
            <IconChev size={16} stroke={cv('text-4')} style={{ transform: importOpen ? 'rotate(90deg)' : 'rotate(0)', transition: 'transform 0.15s' }}/>
          </button>
          {importOpen && (
            <div>
              <SubItem label="Restore Backup" onClick={() => {}} />
              <SubItem label="Import from File" onClick={() => {}} />
            </div>
          )}
          <Section>Other</Section>
          <Item icon={<IconGear size={18} stroke={cv('text-2')}/>} label="Settings" onClick={() => onNavigate('settings')} />
        </div>
        <div style={{ padding: '12px 16px 16px', borderTop: '1px solid ' + cv('divider') }}>
          <button style={{ width: '100%', padding: '11px 14px', borderRadius: 12, border: '1px solid ' + cv('border-strong'), background: cv('bg-elev'), display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8, fontSize: 13, fontWeight: 600, color: cv('text'), cursor: 'pointer' }}>
            <svg width="16" height="16" viewBox="0 0 48 48">
              <path fill="#EA4335" d="M24 9.5c3.54 0 6.7 1.22 9.2 3.61l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"/>
              <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"/>
              <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"/>
              <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"/>
            </svg>
            Sync to Drive
          </button>
        </div>
      </div>
      <div onClick={onClose} style={{ flex: 1, background: 'rgba(0,0,0,0.5)' }} />
    </div>
  );
}

function EntrySheet({ asset, onClose, onSave, accent }) {
  const [value, setValue] = useState2(asset?.value || 0);
  const [change, setChange] = useState2(asset?.change || 0);
  const [name, setName] = useState2(asset?.name || '');
  const isNew = !asset;
  const segBtn = (active) => ({ width: 42, height: '100%', border: 'none', borderRadius: 8, cursor: 'pointer', background: active ? cv('bg-elev') : 'transparent', color: active ? accent : cv('text-4'), fontSize: 18, fontWeight: 700, boxShadow: active ? '0 1px 3px rgba(0,0,0,0.15)' : 'none' });
  const Label = ({ children }) => <div style={{ fontSize: 11, letterSpacing: 1, color: cv('text-3'), fontWeight: 600, textTransform: 'uppercase', margin: '14px 0 6px' }}>{children}</div>;
  const Input = ({ value, onChange, type = 'text', placeholder, prefix, big, formatted }) => (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8, background: cv('bg-alt'), border: '1.5px solid ' + cv('border-strong'), borderRadius: 12, padding: big ? '12px 14px' : '10px 12px' }}>
      {prefix && <span style={{ fontSize: big ? 18 : 14, color: cv('text-4'), fontWeight: 500 }}>{prefix}</span>}
      <input type={type} value={value} placeholder={placeholder} onChange={e => onChange(e.target.value)}
             style={{ flex: 1, border: 'none', outline: 'none', background: 'transparent', fontSize: big ? 20 : 15, fontWeight: big ? 700 : 500, color: cv('text'), letterSpacing: big ? -0.3 : 0, fontFamily: 'inherit', minWidth: 0, width: '100%' }}/>
      {big && formatted && <span style={{ fontSize: 11, color: cv('text-4') }}>{formatted}</span>}
    </div>
  );
  const Chip = ({ children, onClick }) => (
    <button onClick={onClick} style={{ padding: '6px 12px', borderRadius: 100, border: '1px solid ' + cv('border-strong'), background: cv('bg-elev'), fontSize: 12, fontWeight: 600, color: cv('text-2'), cursor: 'pointer' }}>{children}</button>
  );
  return (
    <div style={{ position: 'absolute', inset: 0, zIndex: 60, display: 'flex', flexDirection: 'column', justifyContent: 'flex-end' }}>
      <div onClick={onClose} style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.55)' }} />
      <div style={{ position: 'relative', background: cv('bg-elev'), borderTopLeftRadius: 24, borderTopRightRadius: 24, padding: '12px 20px 20px', boxShadow: '0 -20px 40px rgba(0,0,0,0.3)' }}>
        <div style={{ width: 36, height: 4, background: cv('border-strong'), borderRadius: 2, margin: '0 auto 14px' }} />
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 2 }}>
          <div>
            <div style={{ fontSize: 11, letterSpacing: 1.2, color: accent, fontWeight: 700, textTransform: 'uppercase' }}>{isNew ? 'New entry' : 'Edit · April 2026'}</div>
            <div style={{ fontSize: 20, fontWeight: 700, color: cv('text'), marginTop: 2, letterSpacing: -0.3 }}>{isNew ? 'Add asset or liability' : asset.name}</div>
          </div>
          {!isNew && <div style={{ width: 40, height: 40, borderRadius: 12, background: (asset.color || '#64748b') + '22', color: asset.color || '#64748b', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 16, fontWeight: 800 }}>{asset.name[0].toUpperCase()}</div>}
        </div>
        {isNew && <><Label>Asset name</Label><Input value={name} onChange={setName} placeholder="e.g. Mutual fund"/></>}
        <Label>Current value (₹)</Label>
        <Input value={value} onChange={v => setValue(Number(v) || 0)} type="number" prefix="₹" big formatted={fmtIN(value)} />
        <Label>Change since last month</Label>
        <div style={{ display: 'flex', gap: 10 }}>
          <div style={{ flex: 1 }}><Input value={change} onChange={v => setChange(Number(v) || 0)} type="number" prefix="₹" /></div>
          <div style={{ display: 'flex', background: cv('chip'), borderRadius: 10, padding: 3 }}>
            <button onClick={() => setChange(Math.abs(change))} style={segBtn(change >= 0)}>+</button>
            <button onClick={() => setChange(-Math.abs(change))} style={segBtn(change < 0)}>−</button>
          </div>
        </div>
        {!isNew && (
          <div style={{ display: 'flex', gap: 6, marginTop: 12, flexWrap: 'wrap' }}>
            <Chip onClick={() => setValue(asset.value)}>Same as last month</Chip>
            <Chip onClick={() => { setValue(Math.round(asset.value * 1.05)); setChange(Math.round(asset.value * 0.05));}}>+5%</Chip>
            <Chip onClick={() => { setValue(Math.round(asset.value * 0.95)); setChange(Math.round(asset.value * -0.05));}}>-5%</Chip>
          </div>
        )}
        <div style={{ display: 'flex', gap: 10, marginTop: 18 }}>
          <button onClick={onClose} style={{ flex: 1, padding: '13px', borderRadius: 14, border: '1px solid ' + cv('border-strong'), background: cv('bg-elev'), color: cv('text'), fontSize: 14, fontWeight: 600, cursor: 'pointer' }}>Cancel</button>
          <button onClick={() => onSave({ value, change, name })} style={{ flex: 2, padding: '13px', borderRadius: 14, border: 'none', background: accent, color: '#fff', fontSize: 14, fontWeight: 700, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8, boxShadow: `0 6px 14px ${accent}55` }}>
            <IconSave size={16} stroke="#fff"/>Save entry
          </button>
        </div>
      </div>
    </div>
  );
}

function MonthOverviewScreen({ onBack }) {
  return (
    <div style={{ height: '100%', background: cv('bg'), overflowY: 'auto' }}>
      <ScreenHeader title="Month overview" onBack={onBack} />
      <div style={{ padding: '14px 16px' }}>
        <div style={{ padding: '14px 16px', background: cv('hero-grad'), color: '#fff', borderRadius: 16, marginBottom: 14 }}>
          <div style={{ fontSize: 11, letterSpacing: 1.2, color: 'rgba(255,255,255,0.5)', fontWeight: 700, textTransform: 'uppercase' }}>12-month range</div>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: 12, marginTop: 4 }}>
            <div style={{ fontSize: 22, fontWeight: 700, letterSpacing: -0.3 }}>₹{fmtIN(MONTHLY[MONTHLY.length - 1].total)}</div>
            <div style={{ fontSize: 12, color: '#4ade80', fontWeight: 600 }}>Apr 26</div>
          </div>
          <div style={{ fontSize: 12, color: 'rgba(255,255,255,0.5)', marginTop: 4 }}>Low ₹{fmtIN(Math.min(...MONTHLY.map(m => m.total)))} · High ₹{fmtIN(Math.max(...MONTHLY.map(m => m.total)))}</div>
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {[...MONTHLY].reverse().map((m) => {
            const positive = m.delta >= 0;
            return (
              <div key={m.m} style={{ background: cv('bg-elev'), border: '1px solid ' + cv('border'), borderRadius: 12, padding: '14px 16px', display: 'flex', alignItems: 'center', gap: 12 }}>
                <div style={{ flex: 1 }}>
                  <div style={{ fontSize: 14, fontWeight: 700, color: cv('text') }}>{m.m}</div>
                  {m.delta !== 0 && <div style={{ fontSize: 11, color: positive ? cv('positive') : cv('negative'), marginTop: 2, fontWeight: 600 }}>{positive ? '+' : ''}₹{fmtIN(m.delta)}</div>}
                </div>
                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontSize: 15, fontWeight: 700, color: cv('text'), letterSpacing: -0.2 }}>₹{fmtIN(m.total)}</div>
                  {m.delta !== 0 && <div style={{ fontSize: 11, color: positive ? cv('positive') : cv('negative'), marginTop: 2, fontWeight: 600 }}>{positive ? '↑' : '↓'} {Math.abs(m.pct).toFixed(2)}%</div>}
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

function GraphScreen({ onBack, accent }) {
  const data = MONTHLY;
  const w = 380, h = 280;
  const pad = { l: 36, r: 16, t: 20, b: 28 };
  const iw = w - pad.l - pad.r, ih = h - pad.t - pad.b;
  const max = Math.max(...data.map(d => d.total)) * 1.08;
  const step = iw / (data.length - 1);
  const pts = data.map((d, i) => [pad.l + i * step, pad.t + ih - (d.total / max) * ih]);
  let path = 'M' + pts[0][0] + ',' + pts[0][1];
  for (let i = 0; i < pts.length - 1; i++) {
    const [x0, y0] = pts[i], [x1, y1] = pts[i + 1];
    const cx = (x0 + x1) / 2;
    path += ` C ${cx},${y0} ${cx},${y1} ${x1},${y1}`;
  }
  const area = path + ` L ${pts[pts.length-1][0]},${pad.t+ih} L ${pts[0][0]},${pad.t+ih} Z`;
  const latest = data[data.length - 1];
  const delta = latest.total - data[0].total;
  const StatBlock = ({ label, value, color }) => (
    <div style={{ background: cv('bg-elev'), border: '1px solid ' + cv('border'), borderRadius: 12, padding: '12px 14px' }}>
      <div style={{ fontSize: 10, letterSpacing: 1, color: cv('text-3'), fontWeight: 700, textTransform: 'uppercase' }}>{label}</div>
      <div style={{ fontSize: 18, fontWeight: 700, color: color || cv('text'), letterSpacing: -0.3, marginTop: 4 }}>{value}</div>
    </div>
  );
  return (
    <div style={{ height: '100%', background: cv('bg'), overflowY: 'auto' }}>
      <ScreenHeader title="Graph" onBack={onBack} />
      <div style={{ padding: 16 }}>
        <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', marginBottom: 14 }}>
          <div>
            <div style={{ fontSize: 11, letterSpacing: 1.2, color: cv('text-3'), fontWeight: 700, textTransform: 'uppercase' }}>Current</div>
            <div style={{ fontSize: 24, fontWeight: 800, color: cv('text'), letterSpacing: -0.5 }}>₹{fmtIN(latest.total)}</div>
            <div style={{ fontSize: 12, color: cv('positive'), fontWeight: 600, marginTop: 2 }}>+₹{fmtIN(delta)} ({((delta / data[0].total) * 100).toFixed(1)}%) · 12 mo</div>
          </div>
          <div style={{ display: 'flex', gap: 4, background: cv('chip'), padding: 3, borderRadius: 10 }}>
            {['3M','6M','1Y','ALL'].map((r, i) => (
              <div key={r} style={{ padding: '6px 10px', fontSize: 11, fontWeight: 700, borderRadius: 7, background: i === 2 ? cv('bg-elev') : 'transparent', color: i === 2 ? cv('text') : cv('text-3'), boxShadow: i === 2 ? '0 1px 2px rgba(0,0,0,0.12)' : 'none' }}>{r}</div>
            ))}
          </div>
        </div>
        <div style={{ background: cv('bg-elev'), borderRadius: 16, padding: 16, border: '1px solid ' + cv('border') }}>
          <svg width="100%" viewBox={`0 0 ${w} ${h}`}>
            <defs>
              <linearGradient id="gradNW" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor={accent} stopOpacity="0.25"/>
                <stop offset="100%" stopColor={accent} stopOpacity="0"/>
              </linearGradient>
            </defs>
            {[0,1,2,3,4].map(i => {
              const y = pad.t + (ih / 4) * i;
              const v = max - (max / 4) * i;
              return <g key={i}><line x1={pad.l} y1={y} x2={w - pad.r} y2={y} stroke={cv('divider')} strokeWidth="1"/><text x={pad.l - 6} y={y + 3} fontSize="10" fill="var(--text-4)" textAnchor="end" fontFamily="system-ui">₹{fmtCompact(v)}</text></g>;
            })}
            <path d={area} fill="url(#gradNW)"/>
            <path d={path} fill="none" stroke={accent} strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"/>
            {pts.map((p, i) => <circle key={i} cx={p[0]} cy={p[1]} r={i === pts.length - 1 ? 4.5 : 0} fill={accent} stroke={cv('bg-elev')} strokeWidth="2"/>)}
            {data.map((d, i) => (i % 2 === 0) && <text key={i} x={pts[i][0]} y={h - 8} fontSize="10" fill="var(--text-4)" textAnchor="middle" fontFamily="system-ui">{d.m.slice(0,3)}</text>)}
          </svg>
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, marginTop: 14 }}>
          <StatBlock label="Average" value={`₹${fmtIN(Math.round(data.reduce((s,d)=>s+d.total,0)/data.length))}`}/>
          <StatBlock label="Best month" value={`+${Math.max(...data.map(d=>d.pct)).toFixed(2)}%`} color={cv('positive')}/>
          <StatBlock label="Volatility" value="Low" color={cv('positive')}/>
          <StatBlock label="CAGR (est)" value="+12.4%" color={cv('positive')}/>
        </div>
      </div>
    </div>
  );
}

function AllocationScreen({ onBack, accent }) {
  const top = [...ASSETS].sort((a, b) => b.value - a.value);
  const main = top.slice(0, 5);
  const others = top.slice(5);
  const othersTotal = others.reduce((s, a) => s + a.value, 0);
  const segs = [...main.map(a => ({ name: a.name, value: a.value, color: a.color })), { name: `Others (${others.length})`, value: othersTotal, color: '#94a3b8' }];
  const total = segs.reduce((s, a) => s + a.value, 0);
  const cx = 140, cy = 140, r = 100, rInner = 68;
  let angle = -Math.PI / 2;
  const arcs = segs.map(s => {
    const frac = s.value / total;
    const a0 = angle, a1 = angle + frac * Math.PI * 2;
    angle = a1;
    const large = a1 - a0 > Math.PI ? 1 : 0;
    const [x0, y0] = [cx + r * Math.cos(a0), cy + r * Math.sin(a0)];
    const [x1, y1] = [cx + r * Math.cos(a1), cy + r * Math.sin(a1)];
    const [x0i, y0i] = [cx + rInner * Math.cos(a0), cy + rInner * Math.sin(a0)];
    const [x1i, y1i] = [cx + rInner * Math.cos(a1), cy + rInner * Math.sin(a1)];
    const d = `M ${x0} ${y0} A ${r} ${r} 0 ${large} 1 ${x1} ${y1} L ${x1i} ${y1i} A ${rInner} ${rInner} 0 ${large} 0 ${x0i} ${y0i} Z`;
    return { d, color: s.color };
  });
  return (
    <div style={{ height: '100%', background: cv('bg'), overflowY: 'auto' }}>
      <ScreenHeader title="Allocation" onBack={onBack} />
      <div style={{ padding: 16 }}>
        <div style={{ background: cv('bg-elev'), border: '1px solid ' + cv('border'), borderRadius: 16, padding: 18 }}>
          <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', marginBottom: 6 }}>
            <div style={{ fontSize: 11, letterSpacing: 1.2, color: cv('text-3'), fontWeight: 700, textTransform: 'uppercase' }}>Assets</div>
            <div style={{ fontSize: 16, fontWeight: 700, color: accent, letterSpacing: -0.2 }}>₹{fmtIN(total)}</div>
          </div>
          <svg width="100%" viewBox="0 0 280 280" style={{ display: 'block' }}>
            {arcs.map((a, i) => <path key={i} d={a.d} fill={a.color}/>)}
            <text x={cx} y={cy - 6} textAnchor="middle" fontSize="11" fill="var(--text-3)" fontWeight="600" fontFamily="system-ui">TOTAL</text>
            <text x={cx} y={cy + 16} textAnchor="middle" fontSize="22" fill="var(--text)" fontWeight="800" fontFamily="system-ui" letterSpacing="-0.5">₹{fmtCompact(total)}</text>
          </svg>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginTop: 10 }}>
            {segs.map((s, i) => (
              <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <div style={{ width: 10, height: 10, borderRadius: 3, background: s.color }} />
                <div style={{ flex: 1, fontSize: 13, color: cv('text'), fontWeight: 500 }}>{s.name}</div>
                <div style={{ fontSize: 13, fontWeight: 700, color: cv('text'), letterSpacing: -0.2 }}>{((s.value / total) * 100).toFixed(1)}%</div>
                <div style={{ fontSize: 12, color: cv('text-4'), minWidth: 70, textAlign: 'right' }}>₹{fmtCompact(s.value)}</div>
              </div>
            ))}
          </div>
        </div>
        <div style={{ background: cv('bg-elev'), border: '1px solid ' + cv('border'), borderRadius: 16, padding: 18, marginTop: 14 }}>
          <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', marginBottom: 10 }}>
            <div style={{ fontSize: 11, letterSpacing: 1.2, color: cv('text-3'), fontWeight: 700, textTransform: 'uppercase' }}>Liabilities</div>
            <div style={{ fontSize: 16, fontWeight: 700, color: cv('negative'), letterSpacing: -0.2 }}>−₹{fmtIN(TOTAL_LIAB)}</div>
          </div>
          {LIABILITIES.map(l => (
            <div key={l.id} style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '4px 0' }}>
              <div style={{ width: 10, height: 10, borderRadius: 3, background: l.color }} />
              <div style={{ flex: 1, fontSize: 13, fontWeight: 500, color: cv('text') }}>{l.name}</div>
              <div style={{ fontSize: 13, fontWeight: 700, color: cv('text') }}>{((l.value / TOTAL_LIAB) * 100).toFixed(1)}%</div>
              <div style={{ fontSize: 12, color: cv('text-4'), minWidth: 70, textAlign: 'right' }}>₹{fmtCompact(l.value)}</div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

function NoteScreen({ onBack, accent }) {
  const [text, setText] = useState2('Added bonus of ₹50k to mutual fund this month.\nKotak saving boosted after salary deposit.\n\nNeed to check NPS contribution for Q1.');
  return (
    <div style={{ height: '100%', background: cv('bg-elev'), display: 'flex', flexDirection: 'column' }}>
      <ScreenHeader title="Note · April 2026" onBack={onBack} action={
        <button style={{ background: accent, color: '#fff', border: 'none', padding: '9px 16px', borderRadius: 10, fontSize: 13, fontWeight: 700, display: 'flex', alignItems: 'center', gap: 6, cursor: 'pointer' }}>
          <IconCheck size={14} stroke="#fff" sw={3}/>Save
        </button>
      }/>
      <div style={{ padding: 16, flex: 1, display: 'flex', flexDirection: 'column' }}>
        <div style={{ display: 'flex', gap: 8, marginBottom: 12, flexWrap: 'wrap' }}>
          {['🎯 Goal', '💰 Income', '📉 Dip', '✨ Milestone'].map(t => (
            <button key={t} style={{ padding: '6px 12px', borderRadius: 100, border: '1px solid ' + cv('border-strong'), background: cv('bg-alt'), fontSize: 12, fontWeight: 600, color: cv('text-2'), cursor: 'pointer' }}>{t}</button>
          ))}
        </div>
        <textarea value={text} onChange={e => setText(e.target.value)} placeholder="What changed this month?"
          style={{ flex: 1, border: '1px solid ' + cv('border-strong'), borderRadius: 14, padding: 14, fontSize: 14, lineHeight: 1.6, color: cv('text'), resize: 'none', outline: 'none', fontFamily: 'inherit', background: cv('bg-alt') }}/>
        <div style={{ fontSize: 11, color: cv('text-4'), marginTop: 8, textAlign: 'right' }}>{text.length} characters</div>
      </div>
    </div>
  );
}

function SettingsScreen({ onBack, accent, state, setState, themePref, setThemePref }) {
  const [themePicker, setThemePicker] = useState2(false);
  const [clearDlg, setClearDlg] = useState2(false);
  const themeLabel = { light: 'Light', dark: 'Dark', system: 'System Default' }[themePref];
  const Row = ({ label, sub, right, onClick }) => (
    <button onClick={onClick} style={{ width: '100%', padding: '14px 16px', display: 'flex', alignItems: 'center', background: 'transparent', border: 'none', cursor: onClick ? 'pointer' : 'default', borderBottom: '1px solid ' + cv('divider'), textAlign: 'left' }}>
      <div style={{ flex: 1 }}>
        <div style={{ fontSize: 14, fontWeight: 600, color: cv('text') }}>{label}</div>
        <div style={{ fontSize: 12, color: cv('text-4'), marginTop: 2 }}>{sub}</div>
      </div>
      {right}
    </button>
  );
  const Section = ({ children, title }) => (
    <>
      <div style={{ fontSize: 11, letterSpacing: 1.2, color: accent, fontWeight: 700, textTransform: 'uppercase', padding: '16px 16px 8px' }}>{title}</div>
      <div style={{ background: cv('bg-elev'), borderRadius: 14, margin: '0 16px', border: '1px solid ' + cv('border'), overflow: 'hidden' }}>{children}</div>
    </>
  );
  const Toggle = ({ on, onChange }) => (
    <button onClick={() => onChange(!on)} style={{ width: 40, height: 24, borderRadius: 100, background: on ? accent : cv('border-strong'), border: 'none', cursor: 'pointer', padding: 2, display: 'flex', justifyContent: on ? 'flex-end' : 'flex-start', transition: 'all 0.15s' }}>
      <div style={{ width: 20, height: 20, borderRadius: '50%', background: '#fff', boxShadow: '0 1px 3px rgba(0,0,0,0.2)' }} />
    </button>
  );
  return (
    <div style={{ height: '100%', background: cv('bg'), overflowY: 'auto', position: 'relative' }}>
      <ScreenHeader title="Settings" onBack={onBack} />
      <Section title="Number">
        <Row label="Currency Symbol" sub="₹ Indian Rupee" right={<IconChev size={16} stroke={cv('text-4')}/>} />
        <Row label="Number Grouping" sub="Indian (12 34 567)" right={<IconChev size={16} stroke={cv('text-4')}/>} />
        <Row label="Group Separator" sub="Comma (12,34,567)" right={<IconChev size={16} stroke={cv('text-4')}/>} />
      </Section>
      <Section title="Appearance">
        <Row label="App Theme" sub={themeLabel} right={<IconChev size={16} stroke={cv('text-4')}/>} onClick={() => setThemePicker(true)} />
        <Row label="Accent color" sub={accent} right={<div style={{ width: 20, height: 20, borderRadius: 6, background: accent, border: '2px solid ' + cv('bg-elev'), boxShadow: '0 0 0 1px ' + cv('border-strong') }}/>} />
      </Section>
      <Section title="Data">
        <Row label="Backup & sync" sub="Google Drive" right={<IconChev size={16} stroke={cv('text-4')}/>} />
        <Row label="Export CSV" sub="Download all months" right={<IconChev size={16} stroke={cv('text-4')}/>} />
        <Row label="Reminders" sub="1st of every month, 9:00 AM" right={<Toggle on={true} onChange={() => {}}/>} />
      </Section>
      <Section title="Other">
        <Row label={<span style={{ color: cv('negative') }}>Clear All Data</span>} sub="This cannot be undone" right={<IconTrash size={16} stroke="#dc2626"/>} onClick={() => setClearDlg(true)} />
      </Section>
      <div style={{ textAlign: 'center', padding: '24px 0', fontSize: 11, color: cv('text-4') }}>Net Worth Tracker · v2.0</div>
      {clearDlg && <ClearDataDialog onClose={() => setClearDlg(false)} onConfirm={() => setClearDlg(false)} accent={accent}/>}

      {themePicker && (
        <div style={{ position: 'absolute', inset: 0, zIndex: 60, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div onClick={() => setThemePicker(false)} style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.5)' }}/>
          <div style={{ position: 'relative', background: cv('bg-elev'), borderRadius: 16, width: 300, padding: 20, boxShadow: '0 30px 60px rgba(0,0,0,0.3)' }}>
            <div style={{ fontSize: 17, fontWeight: 700, color: cv('text'), marginBottom: 6 }}>App Theme</div>
            {[{k:'system',l:'System Default'},{k:'light',l:'Light'},{k:'dark',l:'Dark'}].map(o => (
              <button key={o.k} onClick={() => { setThemePref(o.k); setThemePicker(false); }} style={{
                width: '100%', padding: '14px 8px', background: 'transparent', border: 'none', cursor: 'pointer',
                display: 'flex', alignItems: 'center', gap: 12, textAlign: 'left',
              }}>
                <div style={{ width: 20, height: 20, borderRadius: '50%', border: '2px solid ' + (themePref === o.k ? accent : cv('border-strong')), display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  {themePref === o.k && <div style={{ width: 10, height: 10, borderRadius: '50%', background: accent }}/>}
                </div>
                <div style={{ fontSize: 15, color: cv('text'), fontWeight: 500 }}>{o.l}</div>
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

function ClearDataDialog({ onClose, onConfirm, accent }) {
  const [ans, setAns] = useState2('');
  const [err, setErr] = useState2(false);
  const a = 9, b = 5, correct = a + b;
  const submit = () => {
    if (parseInt(ans, 10) === correct) onConfirm();
    else setErr(true);
  };
  return (
    <div style={{ position: 'absolute', inset: 0, zIndex: 80, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 20 }}>
      <div onClick={onClose} style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.55)' }}/>
      <div style={{
        position: 'relative', background: cv('bg-elev'),
        borderRadius: 14, width: '100%', maxWidth: 320,
        padding: '22px 22px 14px',
        border: '1px solid ' + cv('border'),
        boxShadow: '0 30px 60px rgba(0,0,0,0.4)',
      }}>
        <div style={{ fontSize: 20, fontWeight: 700, color: cv('text'), letterSpacing: -0.3 }}>Clear All Data?</div>
        <div style={{ fontSize: 14, color: cv('text-2'), marginTop: 10, lineHeight: 1.5 }}>
          This will permanently delete all your asset data from this device and from Google Drive. Local backup files will not be affected if any. It is recommended to take manual export.
        </div>
        <div style={{ fontSize: 13, color: cv('text-3'), marginTop: 16, fontWeight: 500 }}>
          To confirm, solve: <strong style={{ color: cv('text') }}>{a} + {b} = ?</strong>
        </div>
        <input
          type="number" value={ans} onChange={e => { setAns(e.target.value); setErr(false); }}
          placeholder="Your answer" autoFocus
          style={{
            width: '100%', marginTop: 8, padding: '8px 2px', fontSize: 16,
            background: 'transparent', border: 'none',
            borderBottom: '2px solid ' + (err ? '#ef4444' : cv('border-strong')),
            outline: 'none', color: cv('text'), fontFamily: 'inherit',
          }}
        />
        {err && <div style={{ fontSize: 12, color: '#ef4444', marginTop: 4 }}>Incorrect answer. Try again.</div>}
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 4, marginTop: 22 }}>
          <button onClick={onClose} style={{ padding: '9px 14px', fontSize: 13, fontWeight: 700, letterSpacing: 0.8, color: accent, background: 'transparent', border: 'none', borderRadius: 6, cursor: 'pointer', textTransform: 'uppercase' }}>Cancel</button>
          <button onClick={submit} style={{ padding: '9px 14px', fontSize: 13, fontWeight: 700, letterSpacing: 0.8, color: accent, background: 'transparent', border: 'none', borderRadius: 6, cursor: 'pointer', textTransform: 'uppercase' }}>OK</button>
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { ScreenHeader, SideMenu, EntrySheet, MonthOverviewScreen, GraphScreen, AllocationScreen, NoteScreen, SettingsScreen, ClearDataDialog });
