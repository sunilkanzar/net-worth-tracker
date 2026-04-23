// Asset detail — full-screen view for a single asset
function DetailScreen({ item, monthIdx, onBack, privacy, kind }) {
  const cur = item.values[monthIdx];
  const prev = monthIdx > 0 ? item.values[monthIdx - 1] : cur;
  const first = item.values[0];
  const delta = cur - prev;
  const totalDelta = cur - first;
  const deltaPct = prev ? (delta / prev) * 100 : 0;
  const totalPct = first ? (totalDelta / first) * 100 : 0;
  const isPos = delta >= 0;
  const hide = (t) => privacy ? '••••••' : t;

  const max = Math.max(...item.values);
  const min = Math.min(...item.values) * 0.92;
  const W = 340, H = 160;
  const step = W / (item.values.length - 1);
  const pts = item.values.map((v, i) => {
    const x = i * step;
    const y = H - ((v - min) / (max - min)) * H;
    return [x, y];
  });
  const d = pts.map((p, i) => (i === 0 ? 'M' : 'L') + p[0] + ',' + p[1]).join(' ');
  const areaD = d + ` L${W},${H} L0,${H} Z`;

  return (
    <>
      <StatusBar dark={false} />
      <div style={{
        padding: '6px 12px 0', display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      }}>
        <button className="icon-btn" onClick={onBack}><Icon name="chevronLeft" size={22}/></button>
        <div style={{ fontSize: 14, fontWeight: 500 }}>{item.category}</div>
        <button className="icon-btn"><Icon name="edit" size={18}/></button>
      </div>

      <div className="screen-body" style={{ padding: '8px 20px 40px' }}>
        <div style={{ fontSize: 22, fontWeight: 600, letterSpacing: '-0.01em' }}>{item.name}</div>
        <div style={{ fontSize: 12, color: 'var(--ink-3)', marginTop: 4 }}>
          Last updated · {MONTHS[monthIdx]}
        </div>

        <div className="num" style={{ fontSize: 38, fontWeight: 600, letterSpacing: '-0.02em', marginTop: 20 }}>
          {hide(fmtINR(cur))}
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 4 }}>
          <span className={'chip ' + (isPos ? 'pos' : 'neg')}>
            <Icon name={isPos ? 'arrowUp' : 'arrowDown'} size={10} sw={2.5}/>
            {Math.abs(deltaPct).toFixed(2)}%
          </span>
          <span className="num" style={{ fontSize: 13, color: isPos ? 'var(--pos-ink)' : 'oklch(50% 0.15 25)', fontWeight: 500 }}>
            {isPos ? '+' : '-'}{hide(fmtINR(Math.abs(delta)))}
          </span>
          <span style={{ fontSize: 12, color: 'var(--ink-3)' }}>this month</span>
        </div>

        {/* Chart */}
        <div style={{ marginTop: 26 }}>
          <svg width="100%" height={H + 26} viewBox={`0 0 ${W} ${H + 26}`} preserveAspectRatio="none">
            <defs>
              <linearGradient id="grad" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor="var(--pos)" stopOpacity="0.25"/>
                <stop offset="100%" stopColor="var(--pos)" stopOpacity="0"/>
              </linearGradient>
            </defs>
            <path d={areaD} fill="url(#grad)"/>
            <path d={d} stroke="var(--pos)" strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round"/>
            {pts.map((p, i) => (
              <circle key={i} cx={p[0]} cy={p[1]} r={i === monthIdx ? 5 : 2.5}
                fill={i === monthIdx ? 'var(--pos)' : '#fff'}
                stroke="var(--pos)" strokeWidth={i === monthIdx ? 3 : 1.5}/>
            ))}
            {MONTHS.map((m, i) => (
              <text key={i} x={i * step} y={H + 18} fontSize="9"
                fill="var(--ink-3)" textAnchor="middle" fontFamily="Inter">
                {m.split(' ')[0]}
              </text>
            ))}
          </svg>
        </div>

        {/* Stats grid */}
        <div style={{
          display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, marginTop: 24,
        }}>
          <StatCell label="All time" value={(totalPct >= 0 ? '+' : '') + totalPct.toFixed(1) + '%'}
            sub={fmtINR(Math.abs(totalDelta), { compact: true })} positive={totalPct >= 0}/>
          <StatCell label="6-mo avg" value={fmtINR(item.values.reduce((s,v) => s+v, 0) / item.values.length, { compact: true })}/>
          <StatCell label="Highest" value={fmtINR(max, { compact: true })}/>
          <StatCell label="Lowest" value={fmtINR(Math.min(...item.values), { compact: true })}/>
        </div>

        {/* Monthly history table */}
        <div style={{ marginTop: 28 }}>
          <div style={{ fontSize: 11, color: 'var(--ink-3)', letterSpacing: '0.06em', textTransform: 'uppercase', fontWeight: 500, marginBottom: 10 }}>
            Monthly entries
          </div>
          <div style={{ background: 'var(--bg-raised)', border: '1px solid var(--hair)', borderRadius: 14, overflow: 'hidden' }}>
            {[...item.values].reverse().map((v, i) => {
              const realI = item.values.length - 1 - i;
              const pi = realI - 1 >= 0 ? item.values[realI - 1] : v;
              const d = v - pi;
              const isLast = i === item.values.length - 1;
              return (
                <div key={realI} style={{
                  display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                  padding: '12px 16px',
                  borderBottom: isLast ? 0 : '1px solid var(--hair)',
                }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    <span style={{ fontSize: 13 }}>{MONTHS[realI]}</span>
                    {realI === monthIdx && <span className="chip muted">Current</span>}
                  </div>
                  <div style={{ display: 'flex', alignItems: 'baseline', gap: 10 }}>
                    <span className="num" style={{ fontSize: 13, fontWeight: 500 }}>{fmtINR(v)}</span>
                    {realI > 0 && d !== 0 && (
                      <span className="num" style={{
                        fontSize: 11, color: d >= 0 ? 'var(--pos-ink)' : 'oklch(50% 0.15 25)',
                      }}>
                        {d >= 0 ? '+' : '−'}{fmtINR(Math.abs(d), { compact: true })}
                      </span>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        <div style={{ display: 'flex', gap: 10, marginTop: 20 }}>
          <button className="btn btn-soft" style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6 }}>
            <Icon name="edit" size={14} sw={2}/> Edit value
          </button>
          <button className="btn btn-soft" style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6, color: 'oklch(50% 0.15 25)' }}>
            <Icon name="trash" size={14} sw={2}/> Remove
          </button>
        </div>
      </div>
    </>
  );
}

function StatCell({ label, value, sub, positive }) {
  return (
    <div style={{ padding: 14, background: 'var(--bg-raised)', borderRadius: 14, border: '1px solid var(--hair)' }}>
      <div style={{ fontSize: 11, color: 'var(--ink-3)', letterSpacing: '0.06em', textTransform: 'uppercase', fontWeight: 500 }}>{label}</div>
      <div className="num" style={{
        fontSize: 18, fontWeight: 600, marginTop: 4,
        color: positive === undefined ? 'var(--ink)' : (positive ? 'var(--pos-ink)' : 'oklch(50% 0.15 25)'),
      }}>{value}</div>
      {sub && <div className="num" style={{ fontSize: 11, color: 'var(--ink-3)', marginTop: 2 }}>{sub}</div>}
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Insights sheet (full view)
// ─────────────────────────────────────────────────────────────
function InsightsSheet({ monthIdx, onClose, privacy }) {
  // Allocation donut
  const totalA = ASSETS.reduce((s, a) => s + a.values[monthIdx], 0);
  const catMap = {};
  ASSETS.forEach(a => {
    catMap[a.category] = (catMap[a.category] || 0) + a.values[monthIdx];
  });
  const cats = Object.entries(catMap).map(([n, v]) => ({ name: n, value: v, pct: (v / totalA) * 100 }))
    .sort((a, b) => b.value - a.value);

  const colors = [
    'oklch(74% 0.14 165)',   // emerald
    'oklch(68% 0.14 240)',   // blue
    'oklch(78% 0.14 85)',    // gold
    'oklch(68% 0.15 300)',   // purple
    'oklch(72% 0.14 30)',    // coral
    'oklch(60% 0.04 180)',   // slate
  ];

  // Donut geometry
  const R = 70, SW = 18;
  const C = 2 * Math.PI * R;
  let offset = 0;

  // 6-mo growth
  const h0 = HISTORY[0];
  const hN = HISTORY[monthIdx];
  const n0 = h0.assets - h0.liabilities;
  const nN = hN.assets - hN.liabilities;
  const growthPct = ((nN - n0) / n0) * 100;

  // Top movers (abs)
  const movers = ASSETS.map(a => {
    const c = a.values[monthIdx];
    const p = monthIdx > 0 ? a.values[monthIdx - 1] : c;
    return { ...a, delta: c - p, pct: p ? ((c - p) / p) * 100 : 0 };
  }).filter(m => m.delta !== 0)
    .sort((a, b) => Math.abs(b.delta) - Math.abs(a.delta))
    .slice(0, 4);

  return (
    <div className="sheet" style={{ maxHeight: '92%' }}>
      <div className="sheet-grab"/>
      <div style={{ padding: '18px 20px 8px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div>
          <div style={{ fontSize: 11, color: 'var(--ink-3)', letterSpacing: '0.06em', textTransform: 'uppercase', fontWeight: 500 }}>
            {MONTHS[monthIdx]}
          </div>
          <div style={{ fontWeight: 600, fontSize: 18, marginTop: 2 }}>Insights</div>
        </div>
        <button className="icon-btn" onClick={onClose}><Icon name="close" size={20}/></button>
      </div>

      <div style={{ overflowY: 'auto', flex: 1, padding: '10px 16px 24px' }} className="scroll">
        {/* Allocation */}
        <div style={{ background: 'var(--bg-raised)', border: '1px solid var(--hair)', borderRadius: 16, padding: 18 }}>
          <div style={{ fontSize: 12, color: 'var(--ink-3)', letterSpacing: '0.04em', textTransform: 'uppercase', fontWeight: 500, marginBottom: 12 }}>
            Asset allocation
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 20 }}>
            <svg width="180" height="180" viewBox="0 0 180 180">
              <circle cx="90" cy="90" r={R} fill="none" stroke="var(--hair)" strokeWidth={SW}/>
              {cats.map((c, i) => {
                const len = (c.pct / 100) * C;
                const node = (
                  <circle key={i} cx="90" cy="90" r={R} fill="none"
                    stroke={colors[i % colors.length]} strokeWidth={SW}
                    strokeDasharray={`${len} ${C - len}`}
                    strokeDashoffset={-offset}
                    transform="rotate(-90 90 90)"
                    strokeLinecap="butt"/>
                );
                offset += len;
                return node;
              })}
              <text x="90" y="84" textAnchor="middle" fontSize="10" fill="var(--ink-3)" fontFamily="Inter">Total</text>
              <text x="90" y="102" textAnchor="middle" fontSize="16" fontWeight="600" fill="var(--ink)" fontFamily="JetBrains Mono">
                {privacy ? '••••' : fmtINR(totalA, { compact: true })}
              </text>
            </svg>
            <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 8 }}>
              {cats.map((c, i) => (
                <div key={c.name} style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <div style={{ width: 8, height: 8, borderRadius: 2, background: colors[i % colors.length] }}/>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 12, color: 'var(--ink)', fontWeight: 500 }}>{c.name}</div>
                    <div className="num" style={{ fontSize: 10, color: 'var(--ink-3)' }}>{c.pct.toFixed(1)}%</div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Growth */}
        <div style={{ marginTop: 12, background: 'var(--bg-raised)', border: '1px solid var(--hair)', borderRadius: 16, padding: 18 }}>
          <div style={{ fontSize: 12, color: 'var(--ink-3)', letterSpacing: '0.04em', textTransform: 'uppercase', fontWeight: 500, marginBottom: 6 }}>
            6-month growth
          </div>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: 10 }}>
            <div className="num" style={{ fontSize: 26, fontWeight: 600, color: growthPct >= 0 ? 'var(--pos-ink)' : 'oklch(50% 0.15 25)' }}>
              {growthPct >= 0 ? '+' : ''}{growthPct.toFixed(1)}%
            </div>
            <div className="num" style={{ fontSize: 13, color: 'var(--ink-3)' }}>
              {privacy ? '••••' : fmtINR(nN - n0, { compact: true })}
            </div>
          </div>
          <div style={{ marginTop: 12 }}>
            <Sparkline values={HISTORY.map(h => h.assets - h.liabilities)} width={340} height={40} color="var(--pos)" fill/>
          </div>
        </div>

        {/* Top movers */}
        <div style={{ marginTop: 12, background: 'var(--bg-raised)', border: '1px solid var(--hair)', borderRadius: 16, padding: 18 }}>
          <div style={{ fontSize: 12, color: 'var(--ink-3)', letterSpacing: '0.04em', textTransform: 'uppercase', fontWeight: 500, marginBottom: 12 }}>
            Top movers this month
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {movers.map(m => (
              <div key={m.id} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div style={{ fontSize: 13 }}>{m.name}</div>
                <div style={{ display: 'flex', alignItems: 'baseline', gap: 8 }}>
                  <span className="num" style={{ fontSize: 12, color: 'var(--ink-3)' }}>
                    {m.delta >= 0 ? '+' : '−'}{privacy ? '••' : fmtINR(Math.abs(m.delta), { compact: true })}
                  </span>
                  <span className={'chip ' + (m.delta >= 0 ? 'pos' : 'neg')}>
                    {m.delta >= 0 ? '+' : ''}{m.pct.toFixed(1)}%
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { DetailScreen, InsightsSheet });
