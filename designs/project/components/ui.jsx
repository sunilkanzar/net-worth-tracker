// Icons — simple stroke icons
const Icon = ({ name, size = 20, color = 'currentColor', sw = 1.75 }) => {
  const paths = {
    menu: <><path d="M3 6h18M3 12h18M3 18h18"/></>,
    plus: <><path d="M12 5v14M5 12h14"/></>,
    chevronLeft: <><path d="M15 18l-6-6 6-6"/></>,
    chevronRight: <><path d="M9 6l6 6-6 6"/></>,
    chevronDown: <><path d="M6 9l6 6 6-6"/></>,
    arrowUp: <><path d="M12 19V5M5 12l7-7 7 7"/></>,
    arrowDown: <><path d="M12 5v14M5 12l7 7 7-7"/></>,
    arrowUpRight: <><path d="M7 17L17 7M8 7h9v9"/></>,
    eye: <><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z"/><circle cx="12" cy="12" r="3"/></>,
    eyeOff: <><path d="M17.94 17.94A10.94 10.94 0 0112 19c-6.5 0-10-7-10-7a19.77 19.77 0 013.94-5.06M9.9 4.24A10.94 10.94 0 0112 4c6.5 0 10 7 10 7a19.81 19.81 0 01-2.16 3.19M14.12 14.12a3 3 0 01-4.24-4.24"/><path d="M1 1l22 22"/></>,
    close: <><path d="M18 6L6 18M6 6l12 12"/></>,
    check: <><path d="M20 6L9 17l-5-5"/></>,
    search: <><circle cx="11" cy="11" r="7"/><path d="M21 21l-4.3-4.3"/></>,
    filter: <><path d="M3 5h18M7 12h10M10 19h4"/></>,
    calendar: <><rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/></>,
    edit: <><path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/><path d="M18.5 2.5a2.12 2.12 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/></>,
    trash: <><path d="M3 6h18M8 6V4a2 2 0 012-2h4a2 2 0 012 2v2M19 6l-1 14a2 2 0 01-2 2H8a2 2 0 01-2-2L5 6"/></>,
    home: <><path d="M3 12l9-9 9 9M5 10v10h14V10"/></>,
    pieChart: <><path d="M21.21 15.89A10 10 0 118 2.83"/><path d="M22 12A10 10 0 0012 2v10z"/></>,
    sparkle: <><path d="M12 3l2 5 5 2-5 2-2 5-2-5-5-2 5-2z"/></>,
    wallet: <><path d="M20 12V8H6a2 2 0 01-2-2 2 2 0 012-2h12v4"/><path d="M4 6v12a2 2 0 002 2h14v-4"/><path d="M18 12a2 2 0 000 4h4v-4z"/></>,
    trending: <><path d="M23 6l-9.5 9.5-5-5L1 18"/><path d="M17 6h6v6"/></>,
    user: <><circle cx="12" cy="8" r="4"/><path d="M20 21a8 8 0 10-16 0"/></>,
    bell: <><path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 01-3.46 0"/></>,
    info: <><circle cx="12" cy="12" r="10"/><path d="M12 16v-4M12 8h.01"/></>,
    target: <><circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="6"/><circle cx="12" cy="12" r="2"/></>,
    zap: <><path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/></>,
    wifi: <><path d="M5 12.55a11 11 0 0114 0"/><path d="M1.42 9a16 16 0 0121.16 0"/><path d="M8.53 16.11a6 6 0 016.95 0"/><circle cx="12" cy="20" r="0.5" fill="currentColor"/></>,
    cell: <><rect x="6" y="9" width="2" height="11" rx="0.5" fill="currentColor" stroke="none"/><rect x="10" y="6" width="2" height="14" rx="0.5" fill="currentColor" stroke="none"/><rect x="14" y="3" width="2" height="17" rx="0.5" fill="currentColor" stroke="none"/></>,
    battery: <><rect x="2" y="7" width="18" height="10" rx="2"/><path d="M22 11v2"/><rect x="4" y="9" width="12" height="6" rx="0.5" fill="currentColor" stroke="none"/></>,
  };
  return (
    <svg width={size} height={size} viewBox="0 0 24 24"
      fill="none" stroke={color} strokeWidth={sw}
      strokeLinecap="round" strokeLinejoin="round">
      {paths[name]}
    </svg>
  );
};

// ─────────────────────────────────────────────────────────────
// Status bar
// ─────────────────────────────────────────────────────────────
function StatusBar({ dark = false }) {
  return (
    <div className={'status-bar ' + (dark ? 'on-dark' : '')}>
      <div>9:41</div>
      <div className="status-icons">
        <Icon name="cell" size={16} sw={2} />
        <Icon name="wifi" size={15} sw={2} />
        <Icon name="battery" size={22} sw={1.5} />
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Phone shell
// ─────────────────────────────────────────────────────────────
function Phone({ children, dark = false }) {
  return (
    <div className="phone">
      <div className="phone-screen" style={{ background: dark ? 'var(--dark)' : 'var(--bg)' }}>
        {children}
        <div className={'home-bar ' + (dark ? 'on-dark' : '')} />
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Sparkline — small SVG line
// ─────────────────────────────────────────────────────────────
function Sparkline({ values, width = 72, height = 26, color = 'var(--pos)', fill = false }) {
  if (!values || values.length < 2) return null;
  const min = Math.min(...values);
  const max = Math.max(...values);
  const range = max - min || 1;
  const pad = 2;
  const step = (width - pad * 2) / (values.length - 1);
  const pts = values.map((v, i) => {
    const x = pad + i * step;
    const y = pad + (height - pad * 2) * (1 - (v - min) / range);
    return [x, y];
  });
  const d = pts.map((p, i) => (i === 0 ? 'M' : 'L') + p[0].toFixed(1) + ',' + p[1].toFixed(1)).join(' ');
  const areaD = d + ` L${pts[pts.length-1][0]},${height} L${pts[0][0]},${height} Z`;
  const lastPt = pts[pts.length - 1];
  return (
    <svg width={width} height={height} style={{ overflow: 'visible' }}>
      {fill && <path d={areaD} fill={color} opacity="0.12" />}
      <path d={d} stroke={color} strokeWidth="1.5" fill="none" strokeLinecap="round" strokeLinejoin="round" />
      <circle cx={lastPt[0]} cy={lastPt[1]} r="2" fill={color} />
    </svg>
  );
}

// ─────────────────────────────────────────────────────────────
// Bar chart (net worth history)
// ─────────────────────────────────────────────────────────────
function HistoryBars({ history, active = -1, onSelect, height = 56, onDark = true }) {
  const values = history.map(h => h.assets - h.liabilities);
  const max = Math.max(...values);
  const min = Math.min(...values) * 0.85;
  const barW = 100 / (values.length * 1.6);
  const gap = barW * 0.6;
  return (
    <div style={{ display: 'flex', alignItems: 'flex-end', gap: 5, height }}>
      {values.map((v, i) => {
        const h = ((v - min) / (max - min)) * (height - 8) + 8;
        const isActive = i === (active === -1 ? values.length - 1 : active);
        return (
          <div
            key={i}
            onClick={() => onSelect && onSelect(i)}
            style={{
              flex: 1,
              height: h,
              background: isActive
                ? 'oklch(80% 0.15 90)'
                : (onDark ? '#2a2a25' : '#d6d4cc'),
              borderRadius: '3px 3px 2px 2px',
              cursor: onSelect ? 'pointer' : 'default',
              transition: 'background .15s ease, height .25s ease',
            }}
          />
        );
      })}
    </div>
  );
}

Object.assign(window, { Icon, StatusBar, Phone, Sparkline, HistoryBars });
