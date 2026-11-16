#!/bin/bash
# Kullanım: ./toggle_network.sh <on|off> <device_id> <ios|android>
# Örnek: ./toggle_network.sh off emulator-5554 android
#         ./toggle_network.sh off "A1B2C3D4-..." ios
set -e
export PATH="$HOME/.maestro/bin:$PATH"

ACTION=$1
DEVICE=$2
PLATFORM=$3

if [ -z "$ACTION" ] || [ -z "$DEVICE" ] || [ -z "$PLATFORM" ]; then
  echo "Kullanım: $0 <on|off> <device_id> <ios|android>"
  exit 1
fi

if [ "$PLATFORM" = "android" ]; then
  if [ "$ACTION" = "off" ]; then
    adb -s "$DEVICE" shell svc wifi disable
    adb -s "$DEVICE" shell svc data disable
    echo "Android ağ KAPATILDI: $DEVICE"
  else
    adb -s "$DEVICE" shell svc wifi enable
    adb -s "$DEVICE" shell svc data enable
    echo "Android ağ AÇILDI: $DEVICE"
  fi
elif [ "$PLATFORM" = "ios" ]; then
  # Xcode 14.3+ gerektirir
  if ! xcrun simctl help 2>&1 | grep -q "set_network_condition"; then
    echo "UYARI: iOS network condition Xcode 14.3+ gerektirir. Atlanıyor."
    exit 0
  fi
  if [ "$ACTION" = "off" ]; then
    xcrun simctl set_network_condition "$DEVICE" "100% loss"
    echo "iOS ağ KAPATILDI: $DEVICE"
  else
    xcrun simctl set_network_condition "$DEVICE" clear
    echo "iOS ağ AÇILDI: $DEVICE"
  fi
else
  echo "Platform 'ios' veya 'android' olmalı."
  exit 1
fi
