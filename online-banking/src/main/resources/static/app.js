const $ = (s, r = document) => r.querySelector(s);
const money = new Intl.NumberFormat('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const when = new Intl.DateTimeFormat('en-IN', { day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit' });

async function api(path, method = 'GET', body) {
  const res = await fetch('/api' + path, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: body ? JSON.stringify(body) : undefined,
  });
  const data = res.status === 204 ? null : await res.json().catch(() => null);
  if (!res.ok) {
    if (res.status === 401 && !path.startsWith('/auth/')) showAuth();
    throw new Error(data?.message || 'Something went wrong. Try again.');
  }
  return data;
}

let toastTimer;
function toast(msg) {
  const t = $('#toast');
  t.textContent = msg;
  t.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => t.classList.remove('show'), 3000);
}

function showAuth() { $('#app').hidden = true; $('#auth').hidden = false; }
function showApp() { $('#auth').hidden = true; $('#app').hidden = false; }

function setProfile(p) {
  $('#name').textContent = p.fullName;
  $('#email').textContent = p.email;
  $('#balance').textContent = '₹' + money.format(p.balance);
  $('#copy').textContent = p.accountNumber;
}

const LABELS = { DEPOSIT: 'Deposit', WITHDRAWAL: 'Withdrawal', TRANSFER_IN: 'Received from', TRANSFER_OUT: 'Sent to' };

async function loadLedger() {
  const rows = await api('/transactions');
  const body = $('#ledger');
  body.replaceChildren();
  $('#empty').hidden = rows.length > 0;
  for (const t of rows) {
    const credit = t.type === 'DEPOSIT' || t.type === 'TRANSFER_IN';
    const tr = document.createElement('tr');
    const details = document.createElement('td');
    details.textContent = LABELS[t.type] + (t.counterparty ? ' ' + t.counterparty : '');
    if (t.note && t.type !== 'DEPOSIT' && t.type !== 'WITHDRAWAL') {
      const s = document.createElement('small'); s.textContent = t.note; details.append(s);
    }
    const date = document.createElement('td'); date.textContent = when.format(new Date(t.createdAt));
    const amt = document.createElement('td'); amt.className = 'r ' + (credit ? 'in' : 'out');
    amt.textContent = (credit ? '+' : '−') + money.format(t.amount);
    const bal = document.createElement('td'); bal.className = 'r'; bal.textContent = money.format(t.balanceAfter);
    tr.append(date, details, amt, bal);
    body.append(tr);
  }
}

async function enter(profile) {
  setProfile(profile);
  showApp();
  await loadLedger();
}

// Generic form handler: collects fields, calls the API, shows the error inline.
function wire(formId, handler) {
  const form = $(formId);
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const err = $('.err', form), btn = $('.primary', form);
    err.textContent = '';
    btn.disabled = true;
    try {
      await handler(Object.fromEntries(new FormData(form)), form);
    } catch (ex) {
      err.textContent = ex.message;
    } finally {
      btn.disabled = false;
    }
  });
}

function amountOf(v) {
  const n = Number(String(v).replace(/,/g, ''));
  if (!Number.isFinite(n) || n <= 0) throw new Error('Enter an amount greater than zero.');
  return n;
}

wire('#login-form', async (d) => enter(await api('/auth/login', 'POST', d)));
wire('#register-form', async (d) => enter(await api('/auth/register', 'POST', d)));

wire('#transfer-form', async (d, f) => {
  const p = await api('/transfer', 'POST', { toAccount: d.toAccount.trim(), amount: amountOf(d.amount), note: d.note });
  setProfile(p); f.reset(); await loadLedger(); toast('Money sent.');
});
wire('#deposit-form', async (d, f) => {
  setProfile(await api('/deposit', 'POST', { amount: amountOf(d.amount) }));
  f.reset(); await loadLedger(); toast('Deposit added.');
});
wire('#withdraw-form', async (d, f) => {
  setProfile(await api('/withdraw', 'POST', { amount: amountOf(d.amount) }));
  f.reset(); await loadLedger(); toast('Withdrawal complete.');
});

function tabs(container, onPick) {
  const btns = [...container.querySelectorAll('[role=tab]')];
  btns.forEach(b => b.addEventListener('click', () => {
    btns.forEach(x => x.setAttribute('aria-selected', x === b));
    onPick(b);
  }));
}
tabs($('#auth .switch'), (b) => {
  const login = b.id === 'tab-login';
  $('#login-form').hidden = !login; $('#register-form').hidden = login;
});
tabs($('#actions'), (b) => {
  for (const a of ['transfer', 'deposit', 'withdraw']) $('#' + a + '-form').hidden = a !== b.dataset.a;
});

$('#copy').addEventListener('click', async () => {
  try { await navigator.clipboard.writeText($('#copy').textContent); toast('Account number copied.'); } catch { /* clipboard unavailable */ }
});
$('#logout').addEventListener('click', async () => { await api('/auth/logout', 'POST'); showAuth(); });

// Restore an existing session on page load.
api('/auth/me').then(enter).catch(showAuth);
