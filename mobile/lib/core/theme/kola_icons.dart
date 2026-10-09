import 'package:flutter/material.dart';

/// Jeu d'icônes de l'application, exprimé en icônes Material intégrées.
///
/// La maquette de référence s'appuie sur Ionicons, mais le paquet Flutter
/// correspondant ne compile pas avec le SDK de ce projet (sa version corrigée
/// exige Dart 3.13). Passer par cette table donne le même vocabulaire d'icônes
/// aux écrans, sans dépendance supplémentaire : le jour où l'on remonte le
/// SDK, seul ce fichier change.
class KolaIcons {
  KolaIcons._();

  // --- Navigation et flèches ---
  static const IconData add = Icons.add;
  static const IconData addCircleOutline = Icons.add_circle_outline;
  static const IconData remove = Icons.remove;
  static const IconData close = Icons.close;
  static const IconData checkmark = Icons.check;
  static const IconData arrowBack = Icons.arrow_back;
  static const IconData arrowForward = Icons.arrow_forward;
  static const IconData arrowUp = Icons.arrow_upward;
  static const IconData arrowDown = Icons.arrow_downward;
  static const IconData arrowUpOutline = Icons.north_east;
  static const IconData arrowDownOutline = Icons.south_west;
  static const IconData arrowUpCircleOutline = Icons.arrow_circle_up_outlined;
  static const IconData chevronForward = Icons.chevron_right;
  static const IconData chevronUp = Icons.keyboard_arrow_up;
  static const IconData chevronDown = Icons.keyboard_arrow_down;
  static const IconData swapHorizontalOutline = Icons.swap_horiz;

  // --- États et retours ---
  static const IconData checkmarkCircle = Icons.check_circle;
  static const IconData checkmarkCircleOutline = Icons.check_circle_outline;
  static const IconData closeCircle = Icons.cancel;
  static const IconData closeCircleOutline = Icons.cancel_outlined;
  static const IconData alertCircleOutline = Icons.error_outline;
  static const IconData informationCircleOutline = Icons.info_outline;
  static const IconData helpCircleOutline = Icons.help_outline;
  static const IconData hourglassOutline = Icons.hourglass_empty;
  static const IconData time = Icons.access_time_filled;
  static const IconData timeOutline = Icons.access_time;
  static const IconData cloudOfflineOutline = Icons.cloud_off_outlined;
  static const IconData refreshOutline = Icons.refresh;
  static const IconData pause = Icons.pause;
  static const IconData play = Icons.play_arrow;
  static const IconData repeatOutline = Icons.repeat;

  // --- Sélection ---
  static const IconData checkbox = Icons.check_box;
  static const IconData squareOutline = Icons.check_box_outline_blank;
  static const IconData radioButtonOn = Icons.radio_button_checked;
  static const IconData radioButtonOff = Icons.radio_button_unchecked;

  // --- Argent et comptes ---
  static const IconData walletOutline = Icons.account_balance_wallet_outlined;
  static const IconData wallet = Icons.account_balance_wallet;
  static const IconData cashOutline = Icons.payments_outlined;
  static const IconData cardOutline = Icons.credit_card;
  static const IconData businessOutline = Icons.account_balance_outlined;
  static const IconData receiptOutline = Icons.receipt_long_outlined;
  static const IconData calculatorOutline = Icons.calculate_outlined;
  static const IconData sendOutline = Icons.send_outlined;
  static const IconData storefrontOutline = Icons.storefront_outlined;
  static const IconData trendingUpOutline = Icons.trending_up;
  static const IconData trophyOutline = Icons.emoji_events_outlined;
  static const IconData flashOutline = Icons.bolt_outlined;

  // --- Coffres et sécurité ---
  static const IconData lockClosed = Icons.lock;
  static const IconData lockClosedOutline = Icons.lock_outline;
  static const IconData lockOpenOutline = Icons.lock_open_outlined;
  static const IconData shieldCheckmark = Icons.verified_user;
  static const IconData shieldCheckmarkOutline = Icons.verified_user_outlined;
  static const IconData shieldOutline = Icons.shield_outlined;
  static const IconData fingerPrint = Icons.fingerprint;
  static const IconData keypadOutline = Icons.dialpad;
  static const IconData eyeOutline = Icons.visibility_outlined;
  static const IconData eyeOffOutline = Icons.visibility_off_outlined;

  // --- Identité ---
  static const IconData person = Icons.person;
  static const IconData personOutline = Icons.person_outline;
  static const IconData personCircleOutline = Icons.account_circle_outlined;
  static const IconData peopleOutline = Icons.people_outline;
  static const IconData idCardOutline = Icons.badge_outlined;
  static const IconData documentTextOutline = Icons.description_outlined;
  static const IconData homeOutline = Icons.home_outlined;
  static const IconData mailOutline = Icons.mail_outline;
  static const IconData createOutline = Icons.edit_outlined;
  static const IconData saveOutline = Icons.save_outlined;
  static const IconData logOutOutline = Icons.logout;
  static const IconData settingsOutline = Icons.settings_outlined;

  // --- QR et caméra ---
  static const IconData qrCode = Icons.qr_code_2;
  static const IconData qrCodeOutline = Icons.qr_code_scanner;
  static const IconData scanOutline = Icons.qr_code_scanner_outlined;

  // --- Divers ---
  static const IconData calendarOutline = Icons.calendar_today_outlined;
  static const IconData notificationsOutline = Icons.notifications_outlined;
  static const IconData searchOutline = Icons.search;
  static const IconData cloudUploadOutline = Icons.cloud_upload_outlined;
  static const IconData downloadOutline = Icons.download_outlined;
  static const IconData sparkles = Icons.auto_awesome;
  static const IconData sparklesOutline = Icons.auto_awesome_outlined;
  static const IconData tvOutline = Icons.tv_outlined;
  static const IconData wifiOutline = Icons.wifi;
  static const IconData waterOutline = Icons.water_drop_outlined;
}
