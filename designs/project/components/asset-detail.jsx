// Single Asset Trend screen + long-press menu dialog

const { useState: useS3, useMemo: useM3 } = React;

function AssetLongPressMenu({ asset, onClose, onViewTrend, onDelete, accent }) {
  if (!asset) return null;
  return (
    <div style={{ position: 'absolute', inset: 0, zIndex: 70, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <div onClick={onClose} style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.55)' }} />
      <div style={{
        position: 'relative', background: 'var(--bg-elev)', borderRadius: 16,
        width: 300, padding: '18px 4px 6px',
        boxShadow: '0 30px 60px rgba(0,0,0,0.5)',
        border: '1px solid var(--border)',
      }}>
        <div style={{ padding: '0 18px 10px' }}>
          <div style={{ fontSize: 11, letterSpacing: 1.2, color: accent, fontWeight: 700, textTransform: 'uppercase' }}>Menu</div>
          <div style={{ fontSize: 16, fontWeight: 700, color: 'var(--text)', marginTop: 2, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{asset.name}</div>
        </div>
        <button onClick={() => onViewTrend(asset)} style={menuRowBtn}>
          <IconTrend size={18} stroke="var(--text-2)"/>
          <span style={{ color: 'var(--text)', fontSize: 15, fontWeight: 500 }}>View Trend</span>
        </button>
        <button onClick={() => onDelete(asset)} style={menuRowBtn}>
          <IconTrash size={18} stroke="#ef4444"/>
          <span style={{ color: '#ef4444', fontSize: 15, fontWeight: 500 }}>Delete this month</span>
        </button>
        <button onClick={onClose} style={{ ...menuRowBtn, marginTop: 4, borderTop: '1px solid var(--divider)' }}>
          <IconClose size={18} stroke="var(--text-3)"/>
          <span style={{ color: 'var(--text-3)', fontSize: 14, fontWeight: 500 }}>Cancel</span>
        </button>
      </div>
    </div>
  );
}
const menuRowBtn = {
  width: '100%', padding: '12px 18px', display: 'flex', alignItems: 'center', gap: 14,
  background: 'transparent', border: 'none', cursor: 'pointer', textAlign: 'left',
};

// Local StatBlock (screens.jsx's version is scoped inside GraphScreen)
function StatBlockAD({ label, value, color }) {
  return (
    <div style={{ background: 'var(--bg-elev)', border: '1px solid var(--border)', borderRadius: 12, padding: '12px 14px' }}>
      <div style={{ fontSize: 10, letterSpacing: 1, color: 'var(--text-3)', fontWeight: 700, textTransform: 'uppercase' }}>{label}</div>
      <div style={{ fontSize: 18, fontWeight: 700, color: color || 'var(--text)', letterSpacing: -0.3, marginTop: 4 }}>{value}</div>
    </div>
  );
}

// Deterministic 24-month trend
function genTrendSingle(seed, target) {
  const out = []; let v = target * 0.2;
  const rand = (n) => { const x = Math.sin(seed * 9301 + n * 49297) * 233280; return x - Math.floor(x); };
  for (let i = 0; i < 24; i++) {
    const p = i / 23;
    const t = target * (0.15 + p * 1.0);
    v = v + (t - v) * 0.3 + (rand(i) - 0.5) * target * 0.1;
    v = Math.max(0, v);
    out.push(v);
  }
  out[out.length - 1] = target;
  return out;
}

function SingleAssetTrendScreen({ asset, onBack, accent }) {
  const data = useM3(() => genTrendSingle(ASSETS.findIndex(a => a.id === asset.id) + 1, asset.value), [asset.id]);
  const [cursor, setCursor] = useS3(null);

  const w = 380, h = 520;
  const pad = { l: 40, r: 14, t: 20, b: 36 };
  const iw = w - pad.l - pad.r, ih = h - pad.t - pad.b;
  const max = Math.max(...data) * 1.08, min = 0;
  const step = iw / (data.length - 1);
  const xFor = (i) => pad.l + i * step;
  const yFor = (v) => pad.t + ih - ((v - min) / (max - min)) * ih;

  let path = 'M' + xFor(0) + ',' + yFor(data[0]);
  for (let i = 1; i < data.length; i++) {
    const x0 = xFor(i-1), y0 = yFor(data[i-1]);
    const x1 = xFor(i), y1 = yFor(data[i]);
    const cx = (x0 + x1) / 2;
    path += ` C ${cx},${y0} ${cx},${y1} ${x1},${y1}`;
  }

  const labels = Array.from({ length: 24 }, (_, i) => {
    const d = new Date(2024, 4 + i, 1);
    return (d.getMonth() + 1) + '.' + String(d.getFullYear()).slice(2);
  });

  const onMove = (e) => {
    const svg = e.currentTarget;
    const r = svg.getBoundingClientRect();
    const xPx = ((e.clientX - r.left) / r.width) * w;
    if (xPx < pad.l || xPx > w - pad.r) { setCursor(null); return; }
    const idx = Math.max(0, Math.min(23, Math.round((xPx - pad.l) / step)));
    setCursor(idx);
  };

  return (
    <div style={{ height: '100%', background: 'var(--bg)', overflowY: 'auto' }}>
      <ScreenHeader
        title={asset.name}
        onBack={onBack}
        action={
          <button style={{
            width: 40, height: 40, borderRadius: 12, border: '1px solid var(--border)',
            background: 'var(--bg-elev)', cursor: 'pointer',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <IconFilter size={16} stroke="var(--text)"/>
          </button>
        }
      />
      <div style={{ padding: '8px 16px 16px' }}>
        {/* stat strip */}
        <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', padding: '8px 2px 14px' }}>
          <div>
            <div style={{ fontSize: 11, letterSpacing: 1.2, color: 'var(--text-3)', fontWeight: 700, textTransform: 'uppercase' }}>Current value</div>
            <div style={{ fontSize: 26, fontWeight: 800, color: 'var(--text)', letterSpacing: -0.5, marginTop: 2 }}>
              ₹{fmtIN(asset.value)}
            </div>
            <div style={{ fontSize: 12, color: asset.pct >= 0 ? 'var(--positive)' : 'var(--negative)', fontWeight: 600, marginTop: 2 }}>
              {asset.pct >= 0 ? '↑' : '↓'} {Math.abs(asset.pct).toFixed(2)}% this month
            </div>
          </div>
          <div style={{ display: 'flex', gap: 4, background: 'var(--chip)', padding: 3, borderRadius: 10 }}>
            {['6M','1Y','2Y','ALL'].map((r, i) => (
              <div key={r} style={{
                padding: '6px 10px', fontSize: 11, fontWeight: 700, borderRadius: 7,
                background: i === 3 ? 'var(--bg-elev)' : 'transparent',
                color: i === 3 ? 'var(--text)' : 'var(--text-3)',
              }}>{r}</div>
            ))}
          </div>
        </div>

        <div style={{ background: 'var(--bg-elev)', borderRadius: 16, padding: '10px 4px 6px', border: '1px solid var(--border)' }}>
          <svg width="100%" viewBox={`0 0 ${w} ${h}`} onMouseMove={onMove} onMouseLeave={() => setCursor(null)}
               style={{ display: 'block', touchAction: 'none' }}>
            <defs>
              <linearGradient id="gradSingle" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor={asset.color} stopOpacity="0.3"/>
                <stop offset="100%" stopColor={asset.color} stopOpacity="0"/>
              </linearGradient>
            </defs>
            {[0,1,2,3,4].map(i => {
              const y = pad.t + (ih / 4) * i;
              const v = max - ((max - min) / 4) * i;
              return (
                <g key={i}>
                  <line x1={pad.l} y1={y} x2={w - pad.r} y2={y} stroke="var(--divider)" strokeWidth="1"/>
                  <text x={pad.l - 6} y={y + 3} fontSize="10" fill="var(--text-4)" textAnchor="end" fontFamily="system-ui">
                    {i === 4 ? '₹0' : fmtCompact(v)}
                  </text>
                </g>
              );
            })}
            <path d={path + ` L ${xFor(23)},${pad.t+ih} L ${xFor(0)},${pad.t+ih} Z`} fill="url(#gradSingle)"/>
            <path d={path} fill="none" stroke={asset.color} strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"/>
            {cursor !== null && (
              <>
                <line x1={xFor(cursor)} y1={pad.t} x2={xFor(cursor)} y2={pad.t+ih} stroke={accent} strokeDasharray="3 3" strokeWidth="1.5" opacity="0.7"/>
                <circle cx={xFor(cursor)} cy={yFor(data[cursor])} r="5" fill="var(--bg-elev)" stroke={asset.color} strokeWidth="2.5"/>
              </>
            )}
            {labels.map((l, i) => (i % 4 === 0 || i === 23) && (
              <text key={i} x={xFor(i)} y={h - 12} fontSize="10" fill="var(--text-4)" textAnchor="middle" fontFamily="system-ui">{l}</text>
            ))}
          </svg>
        </div>

        {cursor !== null && (
          <div style={{
            marginTop: 12, padding: '12px 14px', background: 'var(--bg-elev)',
            border: '1px solid var(--border)', borderRadius: 12,
            display: 'flex', alignItems: 'center', gap: 12,
          }}>
            <div style={{ width: 10, height: 10, borderRadius: 3, background: asset.color }}/>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 11, fontWeight: 700, color: 'var(--text-3)', textTransform: 'uppercase', letterSpacing: 1 }}>{labels[cursor]}</div>
              <div style={{ fontSize: 17, fontWeight: 800, color: 'var(--text)', letterSpacing: -0.3 }}>₹{fmtIN(data[cursor])}</div>
            </div>
          </div>
        )}

        {/* stats */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, marginTop: 12 }}>
          <StatBlockAD label="High" value={'₹' + fmtCompact(Math.max(...data))} color="var(--positive)"/>
          <StatBlockAD label="Low" value={'₹' + fmtCompact(Math.min(...data))} color="var(--negative)"/>
          <StatBlockAD label="Average" value={'₹' + fmtCompact(data.reduce((s,v)=>s+v,0)/data.length)} color="var(--text)"/>
          <StatBlockAD label="Total growth" value={'+' + (((data[data.length-1]-data[0])/Math.max(1,data[0]))*100).toFixed(1) + '%'} color="var(--positive)"/>
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { AssetLongPressMenu, SingleAssetTrendScreen });
