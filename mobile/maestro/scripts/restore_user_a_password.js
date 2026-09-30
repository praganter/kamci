// change_password flow'u için güvenlik ağı (onFlowComplete): şifre geçici
// değerde kaldıysa API ile .env'deki değere geri al. Aksi halde sonraki tüm
// USER_A login'leri kırılır. Maestro runScript (GraalJS) — http/json global.
const base = 'http://198.51.100.10:30080';
const temp = 'YeniTestPass456!';
const headers = { 'Content-Type': 'application/json' };

const login = http.post(base + '/auth/login', {
  headers: headers,
  body: JSON.stringify({ email: USER_A_EMAIL, password: temp }),
});

if (login.ok) {
  const token = json(login.body).token;
  const res = http.post(base + '/auth/change-password', {
    headers: { 'Content-Type': 'application/json', Authorization: 'Bearer ' + token },
    body: JSON.stringify({ currentPassword: temp, newPassword: USER_A_PASSWORD }),
  });
  console.log('USER_A şifresi geri alındı: ' + res.status);
} else {
  console.log('USER_A şifresi zaten orijinal (geçici şifreyle login ' + login.status + ')');
}
