// Hero card — dark top card showing net worth
function HeroCard({ history, monthIdx, setMonthIdx, privacy, setPrivacy, onOpenMenu }) {
  const cur = history[monthIdx];
  const prev = monthIdx > 0 ? history[monthIdx - 1] : null;
  const netWorth = cur.assets - cur.liabilities;
  const prevNet = prev ? prev.assets - prev.liabilities : netWorth;
  const delta = netWorth - prevNet;
  const deltaPct = prev ? ((delta / prevNet) * 100) : 0;
  const isPos = delta >= 0;

  const hide = (txt) => privacy ? '••••••' : txt;

  return (
    <div style={{
      margin: '8px 16px 0',
      background: 'var(--dark)',
      borderRadius: 28,
      padding: '18px 20px 22px',
      color: 'var(--dark-ink)',
      position: 'relative',
      overflow: 'hidden',
      boxShadow: '0 1px 0 rgba(255,255,255,0.04) inset',
    }}>
      {/* subtle texture */}
      <div style={{
        position: 'absolute', inset: 0,
        background: 'radial-gradient(900px 300px at 90% -10%, rgba(255,220,150,0.05), transparent 60%)',
        pointerEvents: 'none',
      }}/>

      {/* Top row */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', position: 'relative' }}>
        <button className="icon-btn" onClick={onOpenMenu} style={{ color: 'var(--dark-ink)', marginLeft: -6 }}>
          <Icon name="menu" size={22} />
        </button>
        <div style={{ fontSize: 11, color: 'var(--dark-ink-2)', letterSpacing: '0.08em', textTransform: 'uppercase', fontWeight: 500 }}>
          Net Worth
        </div>
        <button className="icon-btn" onClick={() => setPrivacy(!privacy)} style={{ color: 'var(--dark-ink)', marginRight: -6 }}>
          <Icon name={privacy ? 'eyeOff' : 'eye'} size={18} />
        </button>
      </div>

      {/* Big amount */}
      <div style={{ marginTop: 14, position: 'relative' }}>
        <div style={{ display: 'flex', alignItems: 'baseline', gap: 10, flexWrap: 'wrap' }}>
          <div className="num" style={{ fontSize: 38, fontWeight: 600, letterSpacing: '-0.02em', lineHeight: 1 }}>
            {hide(fmtINR(netWorth))}
          </div>
          <div className={'chip ' + (isPos ? 'dark-pos' : 'neg')} style={{ transform: 'translateY(-4px)' }}>
            <Icon name={isPos ? 'arrowUp' : 'arrowDown'} size={11} sw={2.5} />
            {Math.abs(deltaPct).toFixed(2)}%
          </div>
        </div>
        <div style={{ marginTop: 8, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <span className="num" style={{
              fontSize: 14, fontWeight: 500,
              color: isPos ? 'oklch(82% 0.14 165)' : 'oklch(70% 0.17 25)',
            }}>
              {hide(fmtINR(Math.abs(delta), { sign: false }))}
            </span>
            <span style={{ color: 'var(--dark-ink-3)', fontSize: 12 }}>vs {prev ? prev.month.split(' ')[0] : '—'}</span>
          </div>
        </div>
      </div>

      {/* Stats strip */}
      <div style={{
        marginTop: 18,
        display: 'grid', gridTemplateColumns: '1fr 1fr auto',
        gap: 12, alignItems: 'end',
        paddingTop: 16,
        borderTop: '1px solid var(--dark-hair)',
      }}>
        <div>
          <div style={{ fontSize: 10, color: 'var(--dark-ink-3)', letterSpacing: '0.06em', textTransform: 'uppercase', fontWeight: 500 }}>Assets</div>
          <div className="num" style={{ fontSize: 15, fontWeight: 500, marginTop: 3 }}>{hide(fmtINR(cur.assets, { compact: true }))}</div>
        </div>
        <div>
          <div style={{ fontSize: 10, color: 'var(--dark-ink-3)', letterSpacing: '0.06em', textTransform: 'uppercase', fontWeight: 500 }}>Liabilities</div>
          <div className="num" style={{ fontSize: 15, fontWeight: 500, marginTop: 3 }}>{hide(fmtINR(cur.liabilities, { compact: true }))}</div>
        </div>
        <div style={{ width: 100 }}>
          <HistoryBars history={history} active={monthIdx} onSelect={setMonthIdx} height={36} />
          <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 4 }}>
            <span style={{ fontSize: 9, color: 'var(--dark-ink-3)' }}>{history[0].month.split(' ')[0]}</span>
            <span style={{ fontSize: 9, color: 'var(--dark-ink-3)' }}>{history[history.length-1].month.split(' ')[0]}</span>
          </div>
        </div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Month switcher pill (floating)
// ─────────────────────────────────────────────────────────────
function MonthSwitcher({ monthIdx, setMonthIdx, months, onOpenMonthPicker }) {
  const canPrev = monthIdx > 0;
  const canNext = monthIdx < months.length - 1;
  return (
    <div style={{
      position: 'absolute', left: '50%', bottom: 24, transform: 'translateX(-50%)',
      background: '#fff',
      borderRadius: 999,
      padding: 6,
      display: 'flex', alignItems: 'center', gap: 4,
      boxShadow: '0 10px 30px rgba(0,0,0,0.12), 0 2px 6px rgba(0,0,0,0.08)',
      border: '1px solid var(--hair)',
      zIndex: 5,
    }}>
      <button className="icon-btn" onClick={() => canPrev && setMonthIdx(monthIdx - 1)}
        style={{ color: canPrev ? 'var(--ink)' : 'var(--ink-4)' }}>
        <Icon name="chevronLeft" size={18} />
      </button>
      <button onClick={onOpenMonthPicker} style={{
        appearance: 'none', border: 0, background: 'transparent', cursor: 'pointer',
        padding: '6px 14px', fontWeight: 500, fontSize: 14, color: 'var(--ink)',
        display: 'flex', alignItems: 'center', gap: 6, fontFamily: 'inherit',
      }}>
        <Icon name="calendar" size={14} sw={2} />
        {months[monthIdx]}
      </button>
      <button className="icon-btn" onClick={() => canNext && setMonthIdx(monthIdx + 1)}
        style={{ color: canNext ? 'var(--ink)' : 'var(--ink-4)' }}>
        <Icon name="chevronRight" size={18} />
      </button>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Asset / Liability row
// ─────────────────────────────────────────────────────────────
function AssetRow({ item, monthIdx, totalAssets, privacy, onClick, kind = 'asset' }) {
  const cur = item.values[monthIdx];
  const prev = monthIdx > 0 ? item.values[monthIdx - 1] : cur;
  const delta = cur - prev;
  const deltaPct = prev ? (delta / prev) * 100 : 0;
  const share = totalAssets ? (cur / totalAssets) * 100 : 0;
  const isPos = delta >= 0;
  const spark = item.values.slice(Math.max(0, monthIdx - 5), monthIdx + 1);
  const hide = (t) => privacy ? '••••' : t;

  const color = isPos ? 'var(--pos)' : 'var(--neg)';
  const chipCls = isPos ? 'pos' : 'neg';

  // For liabilities: down is good
  const isLiab = kind === 'liab';
  const dirGood = isLiab ? !isPos : isPos;

  return (
    <div
      onClick={onClick}
      style={{
        background: 'var(--bg-raised)',
        borderRadius: 16,
        padding: '14px 16px',
        display: 'grid',
        gridTemplateColumns: '1fr auto',
        gap: 12,
        alignItems: 'center',
        cursor: 'pointer',
        border: '1px solid var(--hair)',
        transition: 'transform .1s ease, border-color .15s ease',
      }}
    >
      <div style={{ minWidth: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
          <div style={{ fontSize: 15, fontWeight: 500, color: 'var(--ink)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
            {item.name}
          </div>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <span style={{ fontSize: 11, color: 'var(--ink-3)' }}>{item.category}</span>
          <span style={{ width: 3, height: 3, borderRadius: '50%', background: 'var(--ink-4)' }} />
          <span className="num" style={{ fontSize: 11, color: 'var(--ink-3)' }}>{share.toFixed(1)}%</span>
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <Sparkline values={spark} color={dirGood ? 'var(--pos)' : 'var(--neg)'} width={52} height={22} />
        <div style={{ textAlign: 'right', minWidth: 88 }}>
          <div className="num" style={{ fontSize: 15, fontWeight: 600, color: 'var(--ink)' }}>
            {hide(fmtINR(cur))}
          </div>
          <div style={{ display: 'flex', justifyContent: 'flex-end', alignItems: 'center', gap: 4, marginTop: 2 }}>
            {delta === 0 ? (
              <span style={{ fontSize: 11, color: 'var(--ink-4)' }}>no change</span>
            ) : (
              <>
                <Icon name={isPos ? 'arrowUp' : 'arrowDown'} size={10} sw={2.5} color={dirGood ? 'var(--pos-ink)' : 'oklch(50% 0.15 25)'}/>
                <span className="num" style={{ fontSize: 11, fontWeight: 500, color: dirGood ? 'var(--pos-ink)' : 'oklch(50% 0.15 25)' }}>
                  {Math.abs(deltaPct).toFixed(2)}%
                </span>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { HeroCard, MonthSwitcher, AssetRow });
