// English text for the login screen and the patient portal.
//
// To add a language (isiZulu, Sesotho, Afrikaans, ...):
//   1. Copy this file to e.g. zu.js and translate every value. Keep the keys
//      and anything in {curly braces} exactly as they are.
//   2. Add it to LOCALES in src/i18n.js. The language picker appears by itself
//      once there is more than one language.
//   3. Have a native speaker who knows health terms check it before it goes
//      live: patients act on these words.
// Missing keys fall back to English, so a language can be added a bit at a time.
export default {
  'language.name': 'English',
  'language.label': 'Language',

  'login.headline1': 'Your health record,',
  'login.headline2': 'in one place.',
  'login.tagline': 'Universal Digital Health Record connects patients, clinics and pharmacies so care follows you, not your paperwork.',
  'login.title': 'Sign in',
  'login.subtitle': "Choose how you're accessing UDHR.",
  'login.patient': 'Patient',
  'login.staff': 'Healthcare Staff',
  'login.idNumber': 'South African ID number',
  'login.idPlaceholder': '13-digit ID number',
  'login.dateOfBirth': 'Date of birth',
  'login.pin': 'Portal PIN',
  'login.pinPlaceholder': '6-digit PIN from your clinic',
  'login.pinHelp': 'No PIN yet, or forgot it? Ask the front desk at your clinic.',
  'login.submit': 'Sign in',
  'login.submitting': 'Signing in...',

  'date.day': 'Day',
  'date.month': 'Month',
  'date.year': 'Year',
  'date.months': 'January,February,March,April,May,June,July,August,September,October,November,December',

  'idle.title': 'Are you still there?',
  'idle.body': "For your privacy you'll be signed out in about 2 minutes unless you continue.",
  'idle.signOut': 'Sign out now',
  'idle.stay': 'Stay signed in',

  'portal.hi': 'Hi, {name}',
  'portal.there': 'there',
  'portal.subtitle': "Here's where things stand today.",
  'portal.logout': 'Log out',
  'portal.tab.home': 'Home',
  'portal.tab.medicines': 'My medicines',
  'portal.tab.results': 'My results',
  'portal.tab.help': 'Help',
  'portal.page.labs': 'Lab results',
  'portal.page.referral': 'Referral letter',
  'portal.page.symptoms': 'Check symptoms',
  'portal.page.food': 'Check food',

  'portal.nextMedicine': 'Your next medicine',
  'portal.taken': "I've taken it",
  'portal.nothingDue': 'Nothing due right now.',
  'portal.adherence': 'Weekly adherence',
  'portal.notStarted': 'Not started',
  'portal.lastSymptomCheck': 'Last symptom check',
  'portal.activeAlerts': 'Active alerts',
  'portal.healthProfile': 'Your health profile',
  'portal.condition': 'Condition: {name}',
  'portal.allergy': 'Allergy: {name}',
  'portal.quickActions': 'Quick actions',
  'portal.askAssistant': 'Ask the health assistant',
  'portal.checkSymptoms': 'Check my symptoms',
  'portal.checkFood': 'Check food ingredients',
  'portal.clinicalWarnings': "Clinical warnings & doctor's instructions",
  'portal.dietTips': 'Diet and lifestyle tips for you ({count})',
  'portal.allergyWarnings': 'Medication allergy warnings',
  'portal.privacy': 'Your privacy',
  'portal.privacyBody': 'Only the staff involved in your care can open your record, and every time someone does is recorded. To correct something in your record, ask the front desk at your clinic. You can also complain to the Information Regulator (inforeg.org.za).',
  'portal.smsReminders': 'Send me SMS medication reminders (they never name the medicine)',
};
