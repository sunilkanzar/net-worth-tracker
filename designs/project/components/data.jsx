// Data for Net Worth Tracker — based on user's screenshots
// All values in INR (whole rupees)

const ASSETS = [
  { id: 'zerodha',     name: 'Zerodha stock',       value: 3890000, change: 438783, pct: 12.71, color: '#3b82f6', trend: [28,30,29,32,34,33,36,38,37,39] },
  { id: 'mf',          name: 'Mutual fund',          value: 1425000, change: 96857,  pct: 7.29,  color: '#22d3ee', trend: [10,11,12,12,13,13,14,14,14,14] },
  { id: 'epf',         name: 'EPF smart sense',      value: 880699,  change: 0,      pct: 0,     color: '#10b981', trend: [7,7.5,8,8.2,8.4,8.5,8.6,8.7,8.8,8.8] },
  { id: 'usequity',    name: 'US equity INDmoney',   value: 722000,  change: 73000,  pct: 11.25, color: '#a855f7', trend: [5,5.5,6,6.2,6.5,6.7,6.9,7.0,7.1,7.2] },
  { id: 'nps',         name: 'NPS',                  value: 561000,  change: 31705,  pct: 5.99,  color: '#6366f1', trend: [4,4.2,4.5,4.7,4.9,5.1,5.3,5.4,5.5,5.6] },
  { id: 'kotak_sav',   name: 'Kotak saving account', value: 202000,  change: 92162,  pct: 83.91, color: '#ef4444', trend: [1,1.1,1.2,1.3,1.4,1.5,1.6,1.8,1.9,2.0] },
  { id: 'dilip',       name: 'Dilip',                value: 150000,  change: 0,      pct: 0,     color: '#eab308', trend: [1.5,1.5,1.5,1.5,1.5,1.5,1.5,1.5,1.5,1.5] },
  { id: 'itr',         name: 'ITR demand latter',    value: 128000,  change: 0,      pct: 0,     color: '#f97316', trend: [1.28,1.28,1.28,1.28,1.28,1.28,1.28,1.28,1.28,1.28] },
  { id: 'jagruti',     name: 'Jagruti kotak bank',   value: 120000,  change: 0,      pct: 0,     color: '#14b8a6', trend: [1.2,1.2,1.2,1.2,1.2,1.2,1.2,1.2,1.2,1.2] },
  { id: 'parth',       name: 'Parth',                value: 111199,  change: 0,      pct: 0,     color: '#64748b', trend: [1.1,1.1,1.1,1.1,1.1,1.1,1.1,1.1,1.1,1.11] },
  { id: 'icici',       name: 'Icici saving account', value: 100000,  change: 0,      pct: 0,     color: '#84cc16', trend: [1,1,1,1,1,1,1,1,1,1] },
  { id: 'indwallet',   name: 'US equity IND wallet', value: 35000,   change: 14833,  pct: 73.55, color: '#e11d48', trend: [0.1,0.15,0.2,0.22,0.25,0.28,0.3,0.32,0.33,0.35] },
  { id: 'zerodha_fund',name: 'Zerodha fund',         value: 22000,   change: 0,      pct: 0,     color: '#0ea5e9', trend: [0.22,0.22,0.22,0.22,0.22,0.22,0.22,0.22,0.22,0.22] },
  { id: 'cash',        name: 'Cash on hand',         value: 15000,   change: -5000,  pct: -25,   color: '#94a3b8', trend: [0.2,0.18,0.17,0.16,0.15,0.15,0.15,0.15,0.15,0.15] },
];

const LIABILITIES = [
  { id: 'carloan', name: 'Car loan',           value: 658560, change: -12000, pct: -1.79, color: '#ef4444' },
  { id: 'amzcc',   name: 'Amazon credit card', value: 2268,   change: 500,    pct: 28.3,  color: '#f97316' },
];

// Last 12 months of totals
const MONTHLY = [
  { m: 'May 2025',   total: 7548473, delta: 74716,   pct: 1.00 },
  { m: 'Jun 2025',   total: 7606371, delta: 57898,   pct: 0.77 },
  { m: 'Jul 2025',   total: 7756793, delta: 150422,  pct: 1.98 },
  { m: 'Aug 2025',   total: 8104421, delta: 347628,  pct: 4.48 },
  { m: 'Sep 2025',   total: 8422157, delta: 317736,  pct: 3.92 },
  { m: 'Oct 2025',   total: 8458965, delta: 36808,   pct: 0.44 },
  { m: 'Nov 2025',   total: 8377126, delta: -81839,  pct: -0.97 },
  { m: 'Dec 2025',   total: 7261266, delta: -1115860,pct: -13.32 },
  { m: 'Jan 2026',   total: 6799094, delta: -462172, pct: -6.36 },
  { m: 'Feb 2026',   total: 7545692, delta: 746598,  pct: 10.98 },
  { m: 'Mar 2026',   total: 7545692, delta: 0,       pct: 0 },
  { m: 'Apr 2026',   total: 7545692, delta: 746598,  pct: 10.98 },
];

const TOTAL_ASSETS = ASSETS.reduce((s, a) => s + a.value, 0);
const TOTAL_LIAB = LIABILITIES.reduce((s, a) => s + a.value, 0);
const NET_WORTH = TOTAL_ASSETS - TOTAL_LIAB;
const PREV_NET_WORTH = 6799094;
const NET_DELTA = NET_WORTH - PREV_NET_WORTH;
const NET_PCT = (NET_DELTA / PREV_NET_WORTH) * 100;

// Format number with Indian grouping
function fmtIN(n, { sign = false } = {}) {
  const abs = Math.abs(Math.round(n));
  const s = abs.toString();
  let last3 = s.slice(-3);
  const rest = s.slice(0, -3);
  const withCommas = rest
    ? rest.replace(/\B(?=(\d{2})+(?!\d))/g, ',') + ',' + last3
    : last3;
  const signStr = n < 0 ? '-' : (sign && n > 0 ? '+' : '');
  return signStr + withCommas;
}

function fmtCompact(n) {
  const abs = Math.abs(n);
  if (abs >= 10000000) return (n / 10000000).toFixed(2) + ' Cr';
  if (abs >= 100000) return (n / 100000).toFixed(2) + ' L';
  if (abs >= 1000) return (n / 1000).toFixed(1) + 'K';
  return String(n);
}

Object.assign(window, {
  ASSETS, LIABILITIES, MONTHLY,
  TOTAL_ASSETS, TOTAL_LIAB, NET_WORTH, PREV_NET_WORTH, NET_DELTA, NET_PCT,
  fmtIN, fmtCompact,
});
