// User B (freemium, 5 borç limiti) için borç "slotu" yönetimi — Maestro runScript.
//   ACTION=free    → 5 borç varsa bir ONE_TIME borcu siler (form açılabilsin)
//   ACTION=restore → 5'in altındaysa ONE_TIME borç ekleyerek 5'e tamamlar
// reminder_gate (<5 ister) ile debt_limit_gate (=5 ister) sıra bağımsız koşsun
// diye; seed_test_data.sh ile aynı veri şekli.
const base = 'http://198.51.100.10:30080';
const jsonHeaders = { 'Content-Type': 'application/json' };

const login = http.post(base + '/auth/login', {
  headers: jsonHeaders,
  body: JSON.stringify({ email: USER_B_EMAIL, password: USER_B_PASSWORD }),
});
if (!login.ok) throw new Error('User B login başarısız: ' + login.status);
const auth = { Authorization: 'Bearer ' + json(login.body).token };

const now = new Date();
const month = now.getFullYear() + '-' + String(now.getMonth() + 1).padStart(2, '0');
const list = json(
  http.get(base + '/debt/get?isOwner=true&month=' + month, { headers: auth }).body,
);
const debts = [];
(list.categories || []).forEach((c) =>
  (c.debts || []).forEach((d) => debts.push({ debt: d, categoryId: c.categoryId || c.id })),
);
console.log('User B borç sayısı: ' + debts.length);

if (ACTION === 'free' && debts.length >= 5) {
  const target = debts.find((x) => x.debt.paymentPeriodType === 'ONE_TIME') || debts[debts.length - 1];
  const res = http.delete(base + '/debt/delete/' + target.debt.id, { headers: auth });
  console.log('Silindi: ' + target.debt.name + ' → ' + res.status);
} else if (ACTION === 'restore' && debts.length < 5) {
  const categoryId = debts.length > 0
    ? debts[0].categoryId
    : json(http.get(base + '/category/getAll', { headers: auth }).body)[0].id;
  // Backend LocalDateTime bekler (yyyy-MM-ddTHH:mm:ss); tarih-only → 400 body_not_readable
  const today = now.toISOString().slice(0, 10) + 'T00:00:00';
  for (let i = debts.length; i < 5; i++) {
    const res = http.post(base + '/debt/save', {
      headers: Object.assign({}, jsonHeaders, auth),
      body: JSON.stringify({
        name: 'B Borc ' + (i + 1),
        categoryId: categoryId,
        amount: 100 + i,
        currency: 'TRY',
        paymentPeriodType: 'ONE_TIME',
        firstPaymentDate: today,
        autoPayment: false,
      }),
    });
    console.log('Eklendi B Borc ' + (i + 1) + ' → ' + res.status + (res.ok ? '' : ' ' + res.body));
  }
}
