// Home screen — themed via CSS variables

const { useState, useMemo, useRef, useEffect } = React;
const cvar = (n) => `var(--${n})`;

function Sparkline({ data, color = '#22d3ee', width = 64, height = 24, filled = false }) {
  if (!data || data.length < 2) return null;
  const min = Math.min(...data), max = Math.max(...data);
  const range = max - min || 1;
  const step = width / (data.length - 1);
  const pts = data.map((v, i) => [i * step, height - ((v - min) / range) * (height - 4) - 2]);
  const path = pts.map((p, i) => (i === 0 ? 'M' : 'L') + p[0].toFixed(1) + ',' + p[1].toFixed(1)).join(' ');
  const area = path + ` L ${width},${height} L 0,${height} Z`;
  return (
    <svg width={width} height={height} style={{ overflow: 'visible' }}>
      {filled && <path d={area} fill={color} opacity="0.12" />}
      <path d={path} fill="none" stroke={color} strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" />
      <circle cx={pts[pts.length - 1][0]} cy={pts[pts.length - 1][1]} r="2.5" fill={color} />
    </svg>
  );
}

function HeroBars({ data, accent }) {
  const max = Math.max(...data.map(d => d.total));
  const min = Math.min(...data.map(d => d.total));
  const range = max - min || 1;
  return (
    <svg width="92" height="44" viewBox="0 0 92 44" style={{ display: 'block' }}>
      {data.slice(-8).map((d, i) => {
        const h = ((d.total - min) / range) * 36 + 4;
        const y = 44 - h;
        const isLast = i === 7;
        return <rect key={i} x={i * 11.5 + 1} y={y} width="8.5" height={h} rx="1.5" fill={isLast ? accent : 'rgba(255,255,255,0.18)'} />;
      })}
    </svg>
  );
}

function HeroCard({ netWorth, delta, pct, assetsCount, liabCount, accent, onMenu, privacy, onPrivacyToggle }) {
  const isPositive = delta >= 0;
  const deltaColor = isPositive ? '#4ade80' : '#f87171';
  const btn = {
    width: 36, height: 36, borderRadius: 10, display: 'flex', alignItems: 'center', justifyContent: 'center',
    background: cvar('hero-chip-bg'), border: '1px solid ' + cvar('hero-border'), cursor: 'pointer', padding: 0,
  };
  return (
    <div style={{ background: cvar('hero-grad'), borderRadius: 20, padding: '18px 20px 20px', color: cvar('hero-text'), position: 'relative', overflow: 'hidden' }}>
      <div style={{ position: 'absolute', top: -40, right: -40, width: 200, height: 200, borderRadius: '50%', background: `radial-gradient(circle, ${accent}22 0%, transparent 60%)`, pointerEvents: 'none' }} />
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 14 }}>
        <button onClick={onMenu} style={btn}><IconMenu stroke="#fff" sw={2.2} /></button>
        <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
          <button onClick={onPrivacyToggle} style={btn}>
            {privacy ? <IconEyeOff stroke="rgba(255,255,255,0.7)" size={18}/> : <IconEye stroke="rgba(255,255,255,0.7)" size={18}/>}
          </button>
          <div style={{ display: 'flex', alignItems: 'center', gap: 4, padding: '4px 9px', borderRadius: 100, background: isPositive ? 'rgba(74,222,128,0.15)' : 'rgba(248,113,113,0.15)', color: deltaColor, fontSize: 12, fontWeight: 600 }}>
            {isPositive ? <IconArrowUp size={11} stroke={deltaColor} sw={2.5}/> : <IconArrowDn size={11} stroke={deltaColor} sw={2.5}/>}
            {Math.abs(pct).toFixed(2)}%
          </div>
        </div>
      </div>
      <div style={{ fontSize: 11, fontWeight: 600, letterSpacing: 1.5, color: cvar('hero-text-muted'), textTransform: 'uppercase' }}>Net Worth · Apr 2026</div>
      <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', marginTop: 4 }}>
        <div>
          <div style={{ fontSize: 34, fontWeight: 700, letterSpacing: -0.8, lineHeight: 1.1, fontFamily: "'Plus Jakarta Sans', system-ui" }}>
            <span style={{ fontSize: 22, fontWeight: 500, opacity: 0.7, marginRight: 2 }}>₹</span>
            {privacy ? '••••••••' : fmtIN(netWorth)}
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginTop: 8 }}>
            <span style={{ color: deltaColor, fontSize: 14, fontWeight: 600 }}>{isPositive ? '+' : ''}₹{privacy ? '••••' : fmtIN(delta)}</span>
            <span style={{ color: 'rgba(255,255,255,0.4)', fontSize: 12 }}>vs last month</span>
          </div>
        </div>
        <HeroBars data={MONTHLY} accent={accent} />
      </div>
      <div style={{ marginTop: 16, paddingTop: 14, borderTop: '1px solid ' + cvar('hero-border'), display: 'flex', gap: 20 }}>
        <div>
          <div style={{ fontSize: 10, letterSpacing: 1, color: cvar('hero-text-muted'), fontWeight: 600 }}>ASSETS</div>
          <div style={{ fontSize: 14, fontWeight: 600, marginTop: 2 }}>₹{privacy ? '••' : fmtCompact(TOTAL_ASSETS)} <span style={{ opacity: 0.5, fontWeight: 400, fontSize: 11 }}>· {assetsCount}</span></div>
        </div>
        <div style={{ width: 1, background: cvar('hero-border') }} />
        <div>
          <div style={{ fontSize: 10, letterSpacing: 1, color: cvar('hero-text-muted'), fontWeight: 600 }}>LIABILITIES</div>
          <div style={{ fontSize: 14, fontWeight: 600, marginTop: 2, color: '#fca5a5' }}>₹{privacy ? '••' : fmtCompact(TOTAL_LIAB)} <span style={{ opacity: 0.5, fontWeight: 400, fontSize: 11, color: cvar('hero-text-muted') }}>· {liabCount}</span></div>
        </div>
      </div>
    </div>
  );
}

function InsightStrip({ accent }) {
  const topMover = ASSETS.reduce((a, b) => (Math.abs(b.pct) > Math.abs(a.pct) ? b : a));
  const filledCount = ASSETS.filter(a => a.change !== 0 || a.value > 0).length;
  return (
    <div style={{ display: 'flex', gap: 10, padding: '0 16px', marginTop: 14, overflowX: 'auto', scrollbarWidth: 'none' }}>
      <InsightCard label="Top mover" value={topMover.name.length > 12 ? topMover.name.slice(0, 12) + '…' : topMover.name} sub={`+${topMover.pct.toFixed(1)}%`} subColor="var(--positive)" tint={topMover.color} />
      <InsightCard label="Goal 2027" value="75%" sub="₹1 Cr target" subColor={cvar('text-3')} tint={accent} progress={0.75} />
      <InsightCard label="Updated" value={`${filledCount}/${ASSETS.length}`} sub="assets this month" subColor={cvar('text-3')} tint="#94a3b8" />
    </div>
  );
}

function InsightCard({ label, value, sub, subColor, tint, progress }) {
  return (
    <div style={{ minWidth: 140, background: cvar('bg-elev'), borderRadius: 14, padding: '12px 14px', border: '1px solid ' + cvar('border'), flexShrink: 0, position: 'relative', overflow: 'hidden' }}>
      <div style={{ position: 'absolute', top: 0, left: 0, width: 3, height: '100%', background: tint }} />
      <div style={{ fontSize: 10, letterSpacing: 0.8, color: cvar('text-4'), fontWeight: 600, textTransform: 'uppercase' }}>{label}</div>
      <div style={{ fontSize: 16, fontWeight: 700, color: cvar('text'), marginTop: 3, letterSpacing: -0.2 }}>{value}</div>
      <div style={{ fontSize: 11, color: subColor, marginTop: 2, fontWeight: 500 }}>{sub}</div>
      {progress !== undefined && (
        <div style={{ marginTop: 6, height: 3, background: cvar('divider'), borderRadius: 2, overflow: 'hidden' }}>
          <div style={{ width: `${progress * 100}%`, height: '100%', background: tint, borderRadius: 2 }} />
        </div>
      )}
    </div>
  );
}

function AssetRow({ asset, privacy, onEdit, accent, isEmpty, onLongPress }) {
  const positive = asset.change >= 0;
  const changeColor = positive ? cvar('positive') : cvar('negative');
  const pressTimer = useRef(null);
  const pressed = useRef(false);
  const startPress = () => {
    pressed.current = false;
    pressTimer.current = setTimeout(() => {
      pressed.current = true;
      if (onLongPress) onLongPress(asset);
    }, 500);
  };
  const endPress = () => { clearTimeout(pressTimer.current); };
  const onClick = () => {
    if (pressed.current) { pressed.current = false; return; }
    onEdit(asset);
  };
  const onContextMenu = (e) => { e.preventDefault(); if (onLongPress) onLongPress(asset); };

  if (isEmpty) {
    return (
      <button onClick={() => onEdit(asset)} style={{ width: '100%', background: cvar('bg-elev'), border: '1px dashed ' + cvar('border-strong'), borderRadius: 12, padding: '14px 16px', display: 'flex', alignItems: 'center', gap: 12, cursor: 'pointer', textAlign: 'left' }}>
        <div style={{ width: 32, height: 32, borderRadius: 8, background: cvar('chip'), display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <IconPlus size={16} stroke={cvar('text-4')} sw={2.5} />
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: 14, fontWeight: 600, color: cvar('text-2'), overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{asset.name}</div>
          <div style={{ fontSize: 11.5, color: cvar('text-4'), marginTop: 1 }}>Tap to add value for this month</div>
        </div>
        <IconChev size={16} stroke={cvar('text-4')} />
      </button>
    );
  }
  return (
    <button onClick={onClick}
            onMouseDown={startPress} onMouseUp={endPress} onMouseLeave={endPress}
            onTouchStart={startPress} onTouchEnd={endPress}
            onContextMenu={onContextMenu}
            style={{ width: '100%', background: cvar('bg-elev'), border: '1px solid ' + cvar('border'), borderRadius: 12, padding: '12px 14px', display: 'flex', alignItems: 'center', gap: 12, cursor: 'pointer', textAlign: 'left' }}>
      <div style={{ width: 32, height: 32, borderRadius: 8, background: asset.color + '22', color: asset.color, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 13, fontWeight: 700, flexShrink: 0 }}>{asset.name.slice(0, 1).toUpperCase()}</div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 14, fontWeight: 600, color: cvar('text'), overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{asset.name}</div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 2 }}>
          <span style={{ fontSize: 11, fontWeight: 600, color: cvar('text-3') }}>{((asset.value / TOTAL_ASSETS) * 100).toFixed(1)}%</span>
          <span style={{ width: 2, height: 2, background: cvar('text-4'), borderRadius: 1 }} />
          <span style={{ fontSize: 11, color: cvar('text-4') }}>{fmtCompact(asset.value)}</span>
        </div>
      </div>
      <Sparkline data={asset.trend} color={asset.color} width={48} height={22} filled />
      <div style={{ textAlign: 'right', minWidth: 82 }}>
        <div style={{ fontSize: 14, fontWeight: 700, color: cvar('text'), letterSpacing: -0.2 }}>₹{privacy ? '••••' : fmtIN(asset.value)}</div>
        {asset.change !== 0 && <div style={{ fontSize: 11, color: changeColor, fontWeight: 600, marginTop: 1 }}>{positive ? '↑' : '↓'} {Math.abs(asset.pct).toFixed(2)}%</div>}
      </div>
    </button>
  );
}

function LiabilityRow({ item, privacy, onEdit }) {
  const positive = item.change <= 0;
  const color = positive ? cvar('positive') : cvar('negative');
  return (
    <button onClick={() => onEdit(item)} style={{ width: '100%', background: cvar('bg-elev'), border: '1px solid ' + cvar('border'), borderRadius: 12, padding: '12px 14px', display: 'flex', alignItems: 'center', gap: 12, cursor: 'pointer', textAlign: 'left' }}>
      <div style={{ width: 32, height: 32, borderRadius: 8, background: 'rgba(239,68,68,0.15)', color: '#ef4444', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 13, fontWeight: 700 }}>{item.name.slice(0, 1).toUpperCase()}</div>
      <div style={{ flex: 1 }}>
        <div style={{ fontSize: 14, fontWeight: 600, color: cvar('text') }}>{item.name}</div>
        <div style={{ fontSize: 11, color: cvar('text-4'), marginTop: 2 }}>{((item.value / TOTAL_LIAB) * 100).toFixed(1)}% of liabilities</div>
      </div>
      <div style={{ textAlign: 'right' }}>
        <div style={{ fontSize: 14, fontWeight: 700, color: cvar('text'), letterSpacing: -0.2 }}>-₹{privacy ? '••••' : fmtIN(item.value)}</div>
        {item.change !== 0 && <div style={{ fontSize: 11, color, fontWeight: 600, marginTop: 1 }}>{item.change < 0 ? '↓' : '↑'} {Math.abs(item.pct).toFixed(2)}%</div>}
      </div>
    </button>
  );
}

function MonthNav({ month, onPrev, onNext, accent }) {
  const btn = { width: 30, height: 30, borderRadius: '50%', background: 'transparent', border: 'none', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 0 };
  return (
    <div style={{ position: 'absolute', left: '50%', bottom: 16, transform: 'translateX(-50%)', background: cvar('bg-elev'), borderRadius: 100, boxShadow: cvar('shadow'), padding: '6px 6px', display: 'flex', alignItems: 'center', gap: 4, border: '1px solid ' + cvar('border') }}>
      <button onClick={onPrev} style={btn}><IconChevL size={16} stroke={accent} sw={2.2}/></button>
      <div style={{ fontSize: 14, fontWeight: 700, color: cvar('text'), padding: '0 14px', letterSpacing: 0.2, minWidth: 98, textAlign: 'center' }}>{month}</div>
      <button onClick={onNext} style={btn}><IconChev size={16} stroke={accent} sw={2.2}/></button>
    </div>
  );
}

function Fab({ open, onToggle, onAddAsset, onNote, accent }) {
  return (
    <div style={{ position: 'absolute', right: 16, bottom: 70, display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 10, zIndex: 10 }}>
      {open && (
        <>
          <FabItem label="Monthly Note" icon={<IconNote size={18} stroke="#fff"/>} onClick={onNote} />
          <FabItem label="Add Asset"    icon={<IconPlus size={18} stroke="#fff"/>} onClick={onAddAsset} />
        </>
      )}
      <button onClick={onToggle} style={{ width: 56, height: 56, borderRadius: 18, background: open ? '#1f2937' : accent, border: 'none', boxShadow: `0 10px 24px ${accent}55`, display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer', color: '#fff', transition: 'transform 0.2s', transform: open ? 'rotate(45deg)' : 'rotate(0)' }}>
        <IconPlus size={24} stroke="#fff" sw={2.5} />
      </button>
    </div>
  );
}

function FabItem({ label, icon, onClick }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
      <div style={{ background: cvar('bg-elev'), color: cvar('text'), padding: '8px 14px', borderRadius: 10, fontSize: 13, fontWeight: 600, boxShadow: cvar('shadow') }}>{label}</div>
      <button onClick={onClick} style={{ width: 44, height: 44, borderRadius: 14, background: '#1f2937', border: 'none', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>{icon}</button>
    </div>
  );
}

function HomeScreen({ state, setState, accent, onOpen, onEditAsset, onLongPressAsset }) {
  const [fabOpen, setFabOpen] = useState(false);
  const [showLiab, setShowLiab] = useState(true);
  const monthIdx = state.monthIdx;
  const currentMonth = MONTHLY[monthIdx].m;
  const filledAssets = ASSETS.slice(0, 10);
  const emptyAssets  = ASSETS.slice(10);
  const allFilled = filledAssets.length === ASSETS.length;

  return (
    <div style={{ position: 'relative', height: '100%', background: cvar('bg'), overflow: 'hidden' }}>
      <div style={{ height: '100%', overflowY: 'auto', paddingBottom: 100 }}>
        <div style={{ padding: '10px 16px 0' }}>
          <HeroCard netWorth={NET_WORTH} delta={NET_DELTA} pct={NET_PCT} assetsCount={ASSETS.length} liabCount={LIABILITIES.length} accent={accent} onMenu={() => onOpen('menu')} privacy={state.privacy} onPrivacyToggle={() => setState({ ...state, privacy: !state.privacy })} />
        </div>
        <InsightStrip accent={accent} />
        {!allFilled && (
          <div style={{ padding: '14px 16px 0' }}>
            <button style={{ width: '100%', background: cvar('bg-elev'), border: '1px solid ' + cvar('border'), borderRadius: 12, padding: '11px 14px', display: 'flex', alignItems: 'center', gap: 10, cursor: 'pointer' }}>
              <div style={{ width: 32, height: 32, borderRadius: 8, background: accent + '18', color: accent, display: 'flex', alignItems: 'center', justifyContent: 'center' }}><IconCopy size={16} stroke={accent} /></div>
              <div style={{ flex: 1, textAlign: 'left' }}>
                <div style={{ fontSize: 13, fontWeight: 600, color: cvar('text') }}>Copy values from March 2026</div>
                <div style={{ fontSize: 11, color: cvar('text-4'), marginTop: 1 }}>Fill missing {emptyAssets.length} entries from last month</div>
              </div>
              <IconChev size={16} stroke={cvar('text-4')}/>
            </button>
          </div>
        )}
        <div style={{ padding: '18px 16px 0' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 10, padding: '0 2px' }}>
            <div style={{ fontSize: 11, fontWeight: 700, letterSpacing: 1.2, color: cvar('text-3'), textTransform: 'uppercase' }}>Assets · {ASSETS.length}</div>
            <button style={{ background: 'transparent', border: 'none', color: cvar('text-3'), fontSize: 12, fontWeight: 600, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 4 }}>
              <IconFilter size={12} stroke={cvar('text-3')}/>Sort
            </button>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {filledAssets.map(a => <AssetRow key={a.id} asset={a} privacy={state.privacy} onEdit={onEditAsset} accent={accent} onLongPress={onLongPressAsset} />)}
            {emptyAssets.length > 0 && <div style={{ fontSize: 10, letterSpacing: 1, color: cvar('text-4'), fontWeight: 600, padding: '8px 2px 2px', textTransform: 'uppercase' }}>Not yet entered this month</div>}
            {emptyAssets.map(a => <AssetRow key={a.id} asset={a} privacy={state.privacy} onEdit={onEditAsset} accent={accent} onLongPress={onLongPressAsset} isEmpty />)}
          </div>
        </div>
        <div style={{ padding: '20px 16px 0' }}>
          <button onClick={() => setShowLiab(!showLiab)} style={{ width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'space-between', background: 'transparent', border: 'none', cursor: 'pointer', padding: '0 2px 10px' }}>
            <div style={{ fontSize: 11, fontWeight: 700, letterSpacing: 1.2, color: cvar('text-3'), textTransform: 'uppercase' }}>Liabilities · {LIABILITIES.length}</div>
            <IconChevD size={14} stroke={cvar('text-4')} style={{ transform: showLiab ? 'rotate(0)' : 'rotate(-90deg)' }}/>
          </button>
          {showLiab && <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>{LIABILITIES.map(l => <LiabilityRow key={l.id} item={l} privacy={state.privacy} onEdit={onEditAsset} />)}</div>}
        </div>
        <div style={{ height: 80 }} />
      </div>
      <Fab open={fabOpen} onToggle={() => setFabOpen(!fabOpen)} onAddAsset={() => { setFabOpen(false); onOpen('add-asset'); }} onNote={() => { setFabOpen(false); onOpen('note'); }} accent={accent} />
      <MonthNav month={currentMonth} onPrev={() => setState({ ...state, monthIdx: Math.max(0, monthIdx - 1) })} onNext={() => setState({ ...state, monthIdx: Math.min(MONTHLY.length - 1, monthIdx + 1) })} accent={accent} />
      {fabOpen && <div onClick={() => setFabOpen(false)} style={{ position: 'absolute', inset: 0, background: 'rgba(15,23,42,0.35)', zIndex: 5 }} />}
    </div>
  );
}

Object.assign(window, { HomeScreen, Sparkline, HeroCard });
