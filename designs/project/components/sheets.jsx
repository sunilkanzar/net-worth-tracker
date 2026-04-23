// Entry sheet — monthly value entry for each asset/liability
function EntrySheet({ onClose, monthLabel }) {
  const [vals, setVals] = React.useState(() => {
    const v = {};
    ASSETS.forEach(a => v[a.id] = a.values[a.values.length - 1]);
    LIABILITIES.forEach(l => v[l.id] = l.values[l.values.length - 1]);
    return v;
  });
  const [step, setStep] = React.useState('asset'); // asset | liab | review

  const items = step === 'liab' ? LIABILITIES : ASSETS;
  const [focusId, setFocusId] = React.useState(items[0]?.id);

  const totalA = ASSETS.reduce((s, a) => s + (vals[a.id] || 0), 0);
  const totalL = LIABILITIES.reduce((s, l) => s + (vals[l.id] || 0), 0);
  const net = totalA - totalL;

  const setVal = (id, v) => setVals({ ...vals, [id]: v });

  if (step === 'review') {
    const prev = HISTORY[HISTORY.length - 1];
    const prevNet = prev.assets - prev.liabilities;
    const delta = net - prevNet;
    return (
      <div className="sheet">
        <div className="sheet-grab"/>
        <div style={{ padding: '18px 20px 14px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <button className="icon-btn" onClick={() => setStep('liab')}><Icon name="chevronLeft" size={20}/></button>
          <div style={{ fontWeight: 600 }}>Review</div>
          <button className="icon-btn" onClick={onClose}><Icon name="close" size={20}/></button>
        </div>
        <div style={{ padding: '0 20px 20px', overflowY: 'auto', flex: 1 }}>
          <div style={{ fontSize: 12, color: 'var(--ink-3)', marginBottom: 4 }}>{monthLabel} · Net worth</div>
          <div className="num" style={{ fontSize: 34, fontWeight: 600, letterSpacing: '-0.02em' }}>{fmtINR(net)}</div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 4 }}>
            <span className={'chip ' + (delta >= 0 ? 'pos' : 'neg')}>
              <Icon name={delta >= 0 ? 'arrowUp' : 'arrowDown'} size={10} sw={2.5}/>
              {fmtINR(Math.abs(delta), { compact: true })}
            </span>
            <span style={{ fontSize: 12, color: 'var(--ink-3)' }}>vs last month</span>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, marginTop: 20 }}>
            <SummaryStat label="Assets" value={fmtINR(totalA)} count={ASSETS.length}/>
            <SummaryStat label="Liabilities" value={fmtINR(totalL)} count={LIABILITIES.length}/>
          </div>

          <div style={{ marginTop: 20, padding: 14, background: 'var(--bg)', borderRadius: 14, border: '1px solid var(--hair)' }}>
            <div style={{ fontSize: 12, color: 'var(--ink-2)', lineHeight: 1.5 }}>
              <Icon name="info" size={14} sw={2} color="var(--ink-3)"/>
              <span style={{ marginLeft: 8, verticalAlign: 'middle' }}>You can edit any value later from the asset's detail page.</span>
            </div>
          </div>
        </div>
        <div style={{ padding: 16, borderTop: '1px solid var(--hair)', display: 'flex', gap: 10 }}>
          <button className="btn btn-soft" style={{ flex: 1 }} onClick={() => setStep('asset')}>Edit</button>
          <button className="btn btn-primary" style={{ flex: 2 }} onClick={onClose}>
            <Icon name="check" size={16} sw={2.5} color="#fff"/> <span style={{ marginLeft: 8 }}>Save entry</span>
          </button>
        </div>
      </div>
    );
  }

  const stepTitle = step === 'asset' ? 'Assets' : 'Liabilities';
  const stepN = step === 'asset' ? 1 : 2;

  return (
    <div className="sheet">
      <div className="sheet-grab"/>
      <div style={{ padding: '18px 20px 8px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div>
          <div style={{ fontSize: 11, color: 'var(--ink-3)', letterSpacing: '0.06em', textTransform: 'uppercase', fontWeight: 500 }}>
            Step {stepN} of 2 · {monthLabel}
          </div>
          <div style={{ fontWeight: 600, fontSize: 18, marginTop: 2 }}>Update {stepTitle.toLowerCase()}</div>
        </div>
        <button className="icon-btn" onClick={onClose}><Icon name="close" size={20}/></button>
      </div>

      {/* Progress */}
      <div style={{ padding: '0 20px 14px' }}>
        <div style={{ height: 3, background: 'var(--hair)', borderRadius: 2, overflow: 'hidden' }}>
          <div style={{
            width: step === 'asset' ? '50%' : '100%',
            height: '100%', background: 'var(--ink)',
            transition: 'width .3s ease',
          }}/>
        </div>
      </div>

      <div style={{ overflowY: 'auto', flex: 1, padding: '0 16px 16px' }} className="scroll">
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {items.map(item => (
            <EntryRow
              key={item.id}
              item={item}
              value={vals[item.id]}
              prev={item.values[item.values.length - 2]}
              focused={focusId === item.id}
              onFocus={() => setFocusId(item.id)}
              onChange={(v) => setVal(item.id, v)}
            />
          ))}
          <button style={{
            appearance: 'none', border: '1px dashed var(--ink-4)', background: 'transparent',
            borderRadius: 16, padding: 14, color: 'var(--ink-3)', fontSize: 13, fontFamily: 'inherit',
            cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6,
          }}>
            <Icon name="plus" size={14} sw={2}/> Add new {step === 'liab' ? 'liability' : 'asset'}
          </button>
        </div>
      </div>

      <div style={{ padding: 16, borderTop: '1px solid var(--hair)', display: 'flex', gap: 10, background: 'var(--bg-raised)' }}>
        {step === 'liab' && (
          <button className="btn btn-soft" style={{ flex: 1 }} onClick={() => setStep('asset')}>Back</button>
        )}
        <button className="btn btn-primary" style={{ flex: 2 }} onClick={() => setStep(step === 'asset' ? 'liab' : 'review')}>
          {step === 'asset' ? 'Continue to liabilities' : 'Review'}
        </button>
      </div>
    </div>
  );
}

function SummaryStat({ label, value, count }) {
  return (
    <div style={{ padding: 14, background: 'var(--bg)', borderRadius: 14, border: '1px solid var(--hair)' }}>
      <div style={{ fontSize: 11, color: 'var(--ink-3)', letterSpacing: '0.06em', textTransform: 'uppercase', fontWeight: 500 }}>{label}</div>
      <div className="num" style={{ fontSize: 18, fontWeight: 600, marginTop: 4 }}>{value}</div>
      <div style={{ fontSize: 11, color: 'var(--ink-3)', marginTop: 2 }}>{count} items</div>
    </div>
  );
}

function EntryRow({ item, value, prev, focused, onFocus, onChange }) {
  const delta = value - prev;
  const pctC = prev ? (delta / prev) * 100 : 0;
  const showDelta = Math.abs(delta) > 0.5;
  return (
    <div onClick={onFocus} style={{
      background: focused ? 'var(--bg-raised)' : 'var(--bg-raised)',
      borderRadius: 14,
      padding: '12px 14px',
      border: '1px solid ' + (focused ? 'var(--ink)' : 'var(--hair)'),
      display: 'grid', gridTemplateColumns: '1fr auto', alignItems: 'center',
      gap: 10,
      cursor: 'text',
      transition: 'border-color .15s ease',
    }}>
      <div style={{ minWidth: 0 }}>
        <div style={{ fontSize: 14, fontWeight: 500, color: 'var(--ink)' }}>{item.name}</div>
        <div style={{ fontSize: 11, color: 'var(--ink-3)', marginTop: 2 }}>
          Prev: <span className="num">{fmtINR(prev, { compact: true })}</span>
          {showDelta && (
            <>
              <span style={{ margin: '0 6px', color: 'var(--ink-4)' }}>·</span>
              <span className="num" style={{ color: delta >= 0 ? 'var(--pos-ink)' : 'oklch(50% 0.15 25)' }}>
                {delta >= 0 ? '+' : ''}{pctC.toFixed(1)}%
              </span>
            </>
          )}
        </div>
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
        <span style={{ color: 'var(--ink-3)', fontSize: 13 }}>₹</span>
        <input
          className="num"
          type="text"
          inputMode="numeric"
          value={value || ''}
          onChange={(e) => onChange(parseInt(e.target.value.replace(/\D/g, '') || '0', 10))}
          onFocus={onFocus}
          style={{
            appearance: 'none', border: 0, background: 'transparent',
            fontSize: 15, fontWeight: 600, color: 'var(--ink)',
            fontFamily: 'JetBrains Mono, monospace',
            width: 110, textAlign: 'right', padding: '4px 2px',
            borderRadius: 4,
          }}
        />
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Month picker
// ─────────────────────────────────────────────────────────────
function MonthPickerSheet({ monthIdx, setMonthIdx, onClose }) {
  return (
    <div className="sheet" style={{ maxHeight: '60%' }}>
      <div className="sheet-grab"/>
      <div style={{ padding: '18px 20px 10px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div style={{ fontWeight: 600, fontSize: 17 }}>Select month</div>
        <button className="icon-btn" onClick={onClose}><Icon name="close" size={20}/></button>
      </div>
      <div style={{ padding: '0 16px 20px', overflowY: 'auto' }}>
        {[...MONTHS].reverse().map((m, i) => {
          const idx = MONTHS.length - 1 - i;
          const active = idx === monthIdx;
          const h = HISTORY[idx];
          const net = h.assets - h.liabilities;
          return (
            <div key={m} onClick={() => { setMonthIdx(idx); onClose(); }} style={{
              padding: '14px 14px', borderRadius: 12, cursor: 'pointer',
              background: active ? 'var(--bg)' : 'transparent',
              display: 'flex', alignItems: 'center', justifyContent: 'space-between',
              marginBottom: 2,
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                <div style={{
                  width: 20, height: 20, borderRadius: '50%',
                  border: '2px solid ' + (active ? 'var(--ink)' : 'var(--ink-4)'),
                  background: active ? 'var(--ink)' : 'transparent',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                }}>
                  {active && <div style={{ width: 6, height: 6, borderRadius: '50%', background: '#fff' }}/>}
                </div>
                <div>
                  <div style={{ fontSize: 14, fontWeight: 500 }}>{m}</div>
                  <div className="num" style={{ fontSize: 12, color: 'var(--ink-3)', marginTop: 1 }}>{fmtINR(net, { compact: true })}</div>
                </div>
              </div>
              {idx === MONTHS.length - 1 && <span className="chip pos">Latest</span>}
            </div>
          );
        })}
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Menu / drawer
// ─────────────────────────────────────────────────────────────
function MenuSheet({ onClose }) {
  const items = [
    { icon: 'home', label: 'Home', active: true },
    { icon: 'pieChart', label: 'Insights' },
    { icon: 'target', label: 'Goals' },
    { icon: 'calendar', label: 'Monthly history' },
    { icon: 'bell', label: 'Reminders' },
    { icon: 'user', label: 'Profile' },
  ];
  return (
    <div className="sheet" style={{ maxHeight: '78%' }}>
      <div className="sheet-grab"/>
      <div style={{ padding: '18px 20px 10px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div style={{ fontWeight: 600, fontSize: 17 }}>Menu</div>
        <button className="icon-btn" onClick={onClose}><Icon name="close" size={20}/></button>
      </div>
      <div style={{ padding: '0 16px 20px' }}>
        <div style={{
          padding: 16, background: 'var(--bg)', borderRadius: 14,
          display: 'flex', alignItems: 'center', gap: 12, marginBottom: 14,
        }}>
          <div style={{
            width: 44, height: 44, borderRadius: '50%',
            background: 'var(--ink)', color: '#fff',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontWeight: 600, fontSize: 16,
          }}>A</div>
          <div>
            <div style={{ fontSize: 14, fontWeight: 500 }}>Your tracker</div>
            <div style={{ fontSize: 12, color: 'var(--ink-3)' }}>14 assets · 2 liabilities</div>
          </div>
        </div>
        {items.map((it, i) => (
          <div key={i} onClick={onClose} style={{
            display: 'flex', alignItems: 'center', gap: 14,
            padding: '12px 10px', cursor: 'pointer',
            color: it.active ? 'var(--ink)' : 'var(--ink-2)',
            fontWeight: it.active ? 600 : 400,
            fontSize: 14,
          }}>
            <Icon name={it.icon} size={18} sw={1.8}/>
            {it.label}
          </div>
        ))}
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Sort sheet
// ─────────────────────────────────────────────────────────────
function SortSheet({ onClose }) {
  const [active, setActive] = React.useState('value');
  const opts = [
    { id: 'value', label: 'Value (high to low)' },
    { id: 'change', label: '% change' },
    { id: 'name', label: 'Name' },
    { id: 'category', label: 'Category' },
  ];
  return (
    <div className="sheet" style={{ maxHeight: '50%' }}>
      <div className="sheet-grab"/>
      <div style={{ padding: '18px 20px 10px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div style={{ fontWeight: 600, fontSize: 17 }}>Sort by</div>
        <button className="icon-btn" onClick={onClose}><Icon name="close" size={20}/></button>
      </div>
      <div style={{ padding: '0 16px 20px' }}>
        {opts.map(o => (
          <div key={o.id} onClick={() => { setActive(o.id); onClose(); }} style={{
            padding: '14px 14px', borderRadius: 12, cursor: 'pointer',
            background: active === o.id ? 'var(--bg)' : 'transparent',
            display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          }}>
            <span style={{ fontSize: 14 }}>{o.label}</span>
            {active === o.id && <Icon name="check" size={16} sw={2.5}/>}
          </div>
        ))}
      </div>
    </div>
  );
}

Object.assign(window, { EntrySheet, MonthPickerSheet, MenuSheet, SortSheet });
