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

  /// True if the current user is the inviter (owner) of this household.
  final bool isOwner;

  /// The other person in the household, from the current user's perspective.
  String get partnerEmailForCurrentUser => isOwner ? partnerEmail : ownerEmail;
}
