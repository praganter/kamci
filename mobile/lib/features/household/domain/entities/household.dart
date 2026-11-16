enum HouseholdStatus { pending, accepted }

class Household {
  const Household({
    required this.id,
    required this.ownerId,
    required this.ownerEmail,
    required this.partnerId,
    required this.partnerEmail,
    required this.status,
    required this.isOwner,
  });

  final String id;
  final String ownerId;
  final String ownerEmail;
  final String partnerId;
  final String partnerEmail;
  final HouseholdStatus status;

  final bool isOwner;

  String get partnerEmailForCurrentUser => isOwner ? partnerEmail : ownerEmail;
}
