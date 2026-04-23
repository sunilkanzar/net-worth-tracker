// Asset Trends screen — multi-line chart with selectable assets

const { useState: useStateT, useMemo: useMemoT } = React;

// Generate 24 months of trend data per asset (deterministic, based on current value)
function genTrend(seed, target, months = 24, volatility = 0.08) {
  const out = [];
  let v = target * 0.25;
  const rand = (n) => {
    const x = Math.sin(seed * 9301 + n * 49297) * 233280;
    return x - Math.floor(x);
  };
  for (let i = 0; i < months; i++) {
    const progress = i / (months - 1);
    const trendTarget = target * (0.25 + progress * 0.9);
    v = v + (trendTarget - v) * 0.35 + (rand(i) - 0.5) * target * volatility;
    v = Math.max(0, v);
    out.push(v);
  }
  out[out.length - 1] = target;
  return out;
}

function AssetTrendsScreen({ onBack, accent }) {
  const series = useMemoT(() => ASSETS.map((a, i) => ({
    ...a, data: genTrend(i + 1, a.value, 24, a.pct > 20 ? 0.15 : 0.06),
  })), []);

  const [selected, setSelected] = useStateT(() => new Set(series.slice(0, 6).map(s => s.id)));
  const [showPicker, setShowPicker] = useStateT(false);
  const [cursor, setCursor] = useStateT(null); // {x, idx}

  const visible = series.filter(s => selected.has(s.id));

  const w = 380, h = 340;
  const pad = { l: 46, r: 12, t: 16, b: 32 };
  const iw = w - pad.l - pad.r, ih = h - pad.t - pad.b;
  const months = 24;
  const labels = Array.from({ length: months }, (_, i) => {
    const d = new Date(2024, 4 + i, 1);
    return d.toLocaleString('en', { month: 'short' }) + ' ' + String(d.getFullYear()).slice(2);
  });

  const allValues = visible.flatMap(s => s.data);
  const max = Math.max(1, ...allValues) * 1.05;
  const min = 0;
  const step = iw / (months - 1);

  const xFor = (i) => pad.l + i * step;
  const yFor = (v) => pad.t + ih - ((v - min) / (max - min)) * ih;

  const pathFor = (data) => {
    let p = 'M' + xFor(0) + ',' + yFor(data[0]);
    for (let i = 1; i < data.length; i++) {
      const x0 = xFor(i - 1), y0 = yFor(data[i - 1]);
      const x1 = xFor(i), y1 = yFor(data[i]);
      const cx = (x0 + x1) / 2;
      p += ` C ${cx},${y0} ${cx},${y1} ${x1},${y1}`;
    }
    return p;
  };

  const onMove = (e) => {
    const svg = e.currentTarget;
    const rect = svg.getBoundingClientRect();
    const xPx = ((e.clientX - rect.left) / rect.width) * w;
    if (xPx < pad.l || xPx > w - pad.r) { setCursor(null); return; }
    const idx = Math.round((xPx - pad.l) / step);
    const clamped = Math.max(0, Math.min(months - 1, idx));
    setCursor({ idx: clamped, x: xFor(clamped) });
  };

  const yTicks = 5;

  return (
    <div style={{ height: '100%', background: '#f5f6f7', overflowY: 'auto' }}>
      <ScreenHeader
        title="Asset Trends"
        onBack={onBack}
        action={
          <button onClick={() => setShowPicker(true)} style={{
            width: 40, height: 40, borderRadius: 12, border: '1px solid #e2e8f0',
            background: '#fff', cursor: 'pointer',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <IconFilter size={16} stroke="#0f172a"/>
          </button>
        }
      />

      <div style={{ padding: 16 }}>
        {/* summary row */}
        <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', marginBottom: 14 }}>
          <div>
            <div style={{ fontSize: 11, letterSpacing: 1.2, color: '#64748b', fontWeight: 700, textTransform: 'uppercase' }}>
              Plotting {visible.length} asset{visible.length === 1 ? '' : 's'}
            </div>
            <div style={{ fontSize: 18, fontWeight: 800, color: '#0f172a', letterSpacing: -0.3, marginTop: 2 }}>
              24-month trends
            </div>
          </div>
          <div style={{ display: 'flex', gap: 4, background: '#e2e8f0', padding: 3, borderRadius: 10 }}>
            {['6M','1Y','2Y','ALL'].map((r, i) => (
              <div key={r} style={{
                padding: '6px 10px', fontSize: 11, fontWeight: 700, borderRadius: 7,
                background: i === 2 ? '#fff' : 'transparent',
                color: i === 2 ? '#0f172a' : '#64748b',
                boxShadow: i === 2 ? '0 1px 2px rgba(0,0,0,0.08)' : 'none',
              }}>{r}</div>
            ))}
          </div>
        </div>

        {/* chart card */}
        <div style={{ background: '#fff', borderRadius: 16, padding: '16px 10px 12px', border: '1px solid #eef0f2' }}>
          <svg width="100%" viewBox={`0 0 ${w} ${h}`} onMouseMove={onMove} onMouseLeave={() => setCursor(null)}
               style={{ display: 'block', touchAction: 'none' }}>
            {/* gridlines */}
            {Array.from({ length: yTicks + 1 }, (_, i) => {
              const y = pad.t + (ih / yTicks) * i;
              const v = max - ((max - min) / yTicks) * i;
              return (
                <g key={i}>
                  <line x1={pad.l} y1={y} x2={w - pad.r} y2={y} stroke="#f1f5f9" strokeWidth="1"/>
                  <text x={pad.l - 8} y={y + 3} fontSize="10" fill="#94a3b8" textAnchor="end" fontFamily="system-ui">
                    ₹{fmtCompact(v)}
                  </text>
                </g>
              );
            })}

            {/* cursor line */}
            {cursor && (
              <line x1={cursor.x} y1={pad.t} x2={cursor.x} y2={pad.t + ih}
                    stroke={accent} strokeWidth="1.5" strokeDasharray="3 3" opacity="0.7"/>
            )}

            {/* lines */}
            {visible.map(s => (
              <path key={s.id} d={pathFor(s.data)} fill="none" stroke={s.color}
                    strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
                    opacity={cursor ? 0.55 : 1}/>
            ))}

            {/* cursor dots */}
            {cursor && visible.map(s => (
              <circle key={s.id} cx={cursor.x} cy={yFor(s.data[cursor.idx])}
                      r="3.5" fill="#fff" stroke={s.color} strokeWidth="2"/>
            ))}

            {/* x axis labels */}
            {labels.map((l, i) => (i % 4 === 0 || i === months - 1) && (
              <text key={i} x={xFor(i)} y={h - 10} fontSize="10" fill="#94a3b8" textAnchor="middle" fontFamily="system-ui">
                {l}
              </text>
            ))}
          </svg>

          {/* cursor values card */}
          {cursor && (
            <div style={{
              margin: '4px 10px 0', padding: '10px 12px',
              background: '#f8fafc', borderRadius: 10, border: '1px solid #eef0f2',
            }}>
              <div style={{ fontSize: 11, fontWeight: 700, color: '#64748b', textTransform: 'uppercase', letterSpacing: 1, marginBottom: 6 }}>
                {labels[cursor.idx]}
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 4 }}>
                {visible.slice(0, 6).map(s => (
                  <div key={s.id} style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                    <div style={{ width: 8, height: 8, borderRadius: 2, background: s.color, flexShrink: 0 }}/>
                    <span style={{ fontSize: 11, color: '#475569', fontWeight: 500, flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {s.name.length > 14 ? s.name.slice(0, 13) + '…' : s.name}
                    </span>
                    <span style={{ fontSize: 11, fontWeight: 700, color: '#0f172a' }}>₹{fmtCompact(s.data[cursor.idx])}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* legend list */}
        <div style={{ marginTop: 14, display: 'flex', flexDirection: 'column', gap: 6 }}>
          {series.map(s => {
            const active = selected.has(s.id);
            const first = s.data[0], last = s.data[s.data.length - 1];
            const change = last - first;
            const pct = first > 0 ? (change / first) * 100 : 0;
            return (
              <button key={s.id} onClick={() => {
                const n = new Set(selected);
                if (n.has(s.id)) n.delete(s.id); else n.add(s.id);
                setSelected(n);
              }} style={{
                width: '100%', padding: '10px 12px', borderRadius: 10,
                background: active ? '#fff' : 'rgba(255,255,255,0.5)',
                border: '1px solid ' + (active ? '#eef0f2' : 'transparent'),
                display: 'flex', alignItems: 'center', gap: 10, cursor: 'pointer',
                opacity: active ? 1 : 0.5, textAlign: 'left',
              }}>
                <div style={{
                  width: 14, height: 14, borderRadius: 4, background: s.color, flexShrink: 0,
                  boxShadow: active ? '0 0 0 3px ' + s.color + '22' : 'none',
                }}/>
                <div style={{ flex: 1, fontSize: 13, fontWeight: 600, color: '#0f172a' }}>{s.name}</div>
                <div style={{ fontSize: 12, fontWeight: 700, color: '#0f172a' }}>₹{fmtCompact(last)}</div>
                <div style={{ fontSize: 11, color: pct >= 0 ? '#16a34a' : '#dc2626', fontWeight: 600, minWidth: 56, textAlign: 'right' }}>
                  {pct >= 0 ? '↑' : '↓'} {Math.abs(pct).toFixed(1)}%
                </div>
              </button>
            );
          })}
        </div>
      </div>

      {/* asset picker modal */}
      {showPicker && (
        <div style={{ position: 'absolute', inset: 0, zIndex: 60, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div onClick={() => setShowPicker(false)} style={{ position: 'absolute', inset: 0, background: 'rgba(15,23,42,0.5)' }}/>
          <div style={{
            position: 'relative', background: '#fff', borderRadius: 18,
            width: 340, maxHeight: 540, display: 'flex', flexDirection: 'column',
            boxShadow: '0 30px 60px rgba(15,23,42,0.3)',
          }}>
            <div style={{ padding: '18px 18px 12px', borderBottom: '1px solid #f1f5f9' }}>
              <div style={{ fontSize: 16, fontWeight: 700, color: '#0f172a' }}>Select assets to plot</div>
              <div style={{ fontSize: 12, color: '#94a3b8', marginTop: 2 }}>{selected.size} of {series.length} selected</div>
            </div>
            <div style={{ padding: '8px 12px', display: 'flex', gap: 8 }}>
              <button onClick={() => setSelected(new Set(series.map(s => s.id)))}
                      style={pickerBtn(true, accent)}>Select all</button>
              <button onClick={() => setSelected(new Set())}
                      style={pickerBtn(false, accent)}>Clear</button>
            </div>
            <div style={{ padding: '4px 8px 8px', overflowY: 'auto', flex: 1 }}>
              {series.map(s => {
                const on = selected.has(s.id);
                return (
                  <button key={s.id} onClick={() => {
                    const n = new Set(selected);
                    if (n.has(s.id)) n.delete(s.id); else n.add(s.id);
                    setSelected(n);
                  }} style={{
                    width: '100%', padding: '10px 10px', borderRadius: 8,
                    background: 'transparent', border: 'none', cursor: 'pointer',
                    display: 'flex', alignItems: 'center', gap: 10, textAlign: 'left',
                  }}>
                    <div style={{ width: 12, height: 12, borderRadius: 3, background: s.color }}/>
                    <div style={{ flex: 1, fontSize: 14, color: '#0f172a', fontWeight: 500 }}>{s.name}</div>
                    <div style={{
                      width: 20, height: 20, borderRadius: 5,
                      background: on ? accent : '#fff',
                      border: '2px solid ' + (on ? accent : '#cbd5e1'),
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                    }}>
                      {on && <IconCheck size={12} stroke="#fff" sw={3}/>}
                    </div>
                  </button>
                );
              })}
            </div>
            <div style={{ padding: '12px 16px', borderTop: '1px solid #f1f5f9', display: 'flex', gap: 8 }}>
              <button onClick={() => setShowPicker(false)} style={{
                flex: 1, padding: '11px', borderRadius: 10, border: '1px solid #e2e8f0',
                background: '#fff', fontSize: 14, fontWeight: 600, color: '#0f172a', cursor: 'pointer',
              }}>Cancel</button>
              <button onClick={() => setShowPicker(false)} style={{
                flex: 1, padding: '11px', borderRadius: 10, border: 'none',
                background: accent, color: '#fff', fontSize: 14, fontWeight: 700, cursor: 'pointer',
              }}>Show</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function pickerBtn(primary, accent) {
  return {
    flex: 1, padding: '8px 10px', borderRadius: 8,
    background: primary ? accent + '18' : '#f1f5f9',
    color: primary ? accent : '#475569',
    border: 'none', cursor: 'pointer',
    fontSize: 12, fontWeight: 700, letterSpacing: 0.3,
  };
}

Object.assign(window, { AssetTrendsScreen });
