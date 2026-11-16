#!/usr/bin/env python3
"""Paylaşım testleri için temiz zemin: User A'nın tüm kategorilerini siler.

Neden gerekli:
  _create_default_debt kategori çarkının VARSAYILANINI (ilk kategori) seçer.
  User A'da birden fazla kategori varsa borç yanlış kategoriye düşer ve
  device_c_verify_* paylaşılan borcu göremez.
  Ayrıca _create_default_category her koşumda yeni bir "Test Kategorisi"
  bırakır → birikir → aynı isimli 15 kategori arasında flow doğru olanı
  seçemez.

Kategori silinince üyelikleri de gider, yani User C'nin erişimi de sıfırlanır.

Kullanım:
    cd maestro && set -a && . ./.env && set +a && python3 scripts/reset_sharing_state.py
"""
import json
import os
import sys
import urllib.error
import urllib.request

BASE = os.environ.get("API_BASE_URL", "http://localhost:8080")


def req(method, path, token=None, body=None):
    data = json.dumps(body).encode() if body is not None else None
    r = urllib.request.Request(BASE + path, data=data, method=method)
    r.add_header("Content-Type", "application/json")
    if token:
        r.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(r) as resp:
            return resp.status, resp.read().decode()
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode()


def login(email, password):
    st, body = req("POST", "/auth/login", body={"email": email, "password": password})
    if st != 200:
        sys.exit(f"login başarısız ({email}): HTTP {st} {body[:120]}")
    return json.loads(body)["token"]


def main():
    try:
        email = os.environ["USER_A_EMAIL"]
        password = os.environ["USER_A_PASSWORD"]
    except KeyError as e:
        sys.exit(f"{e} tanımsız — önce: set -a && . ./.env && set +a")

    token = login(email, password)
    cats = json.loads(req("GET", "/category/getAll", token)[1])
    print(f"User A kategori: {len(cats)} → siliniyor")

    for c in cats:
        # DELETE /category/delete/{id} → 204 No Content (200 değil)
        st, rb = req("DELETE", f"/category/delete/{c['id']}", token)
        if st not in (200, 204):
            print(f"  HATA {c.get('name')!r}: HTTP {st} {rb[:100]}")

    after = json.loads(req("GET", "/category/getAll", token)[1])
    print(f"kalan: {len(after)}")
    if after:
        sys.exit("temizlik tamamlanamadı")
    print("✅ zemin temiz")


if __name__ == "__main__":
    main()
