import React, { useState, useEffect, useRef } from 'react';
import {
  Activity, Heart, AlertTriangle, Shield, ShieldAlert, User, LogOut, Search, PlusCircle,
  Calendar, MapPin, Phone, CheckCircle, XCircle, FileText, Pill, Compass, Clock,
  Clipboard, RefreshCw, AlertCircle, FileSpreadsheet, Upload, Barcode,
  Menu, Users, CornerUpRight, Package, Building2, Megaphone, MessageCircle, Send, Eye, EyeOff
} from 'lucide-react';
import './App.css';

function App() {
  // Authentication State
  const [token, setToken] = useState(localStorage.getItem('token') || '');
  const [userRole, setUserRole] = useState(localStorage.getItem('role') || '');
  const [userIdNumber, setUserIdNumber] = useState(localStorage.getItem('idNumber') || '');
  const [userName, setUserName] = useState(localStorage.getItem('userName') || '');
  const [mustChangePassword, setMustChangePassword] = useState(localStorage.getItem('mustChangePassword') === 'true');
  const [changePasswordForm, setChangePasswordForm] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });

  const [loginRole, setLoginRole] = useState('patient'); // 'patient' or 'staff'
  const [staffNumber, setStaffNumber] = useState('');
  const [staffPassword, setStaffPassword] = useState('');
  const [patientIdNumber, setPatientIdNumber] = useState('');
  const [patientDob, setPatientDob] = useState('');
  const [showStaffPassword, setShowStaffPassword] = useState(false);
  const [showNewStaffPassword, setShowNewStaffPassword] = useState(false);
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  // Patient Dashboard Data State
  const [patientProfile, setPatientProfile] = useState(null);
  const [patientRecord, setPatientRecord] = useState(null);
  const [healthGuidance, setHealthGuidance] = useState(null);
  const [symptomsList, setSymptomsList] = useState([]);
  const [selectedSymptoms, setSelectedSymptoms] = useState([]);
  const [triageResult, setTriageResult] = useState(null);
  const [triageHistory, setTriageHistory] = useState([]);
  const [loading, setLoading] = useState(false);
  const [myLabResults, setMyLabResults] = useState([]);
  const [patientReferral, setPatientReferral] = useState(null);
  const [patientPortalTab, setPatientPortalTab] = useState('overview'); // 'overview' | 'symptoms' | 'medications' | 'food' | 'labs' | 'referral'

  // Health Assistant (chatbot) state — floating bubble widget, not a tab
  const [chatMessages, setChatMessages] = useState([
    { sender: 'bot', text: "Hi! I'm your health assistant. Describe how you're feeling, or ask me about your medications, allergies, or diet." }
  ]);
  const [chatInput, setChatInput] = useState('');
  const [chatSending, setChatSending] = useState(false);
  const [chatWidgetOpen, setChatWidgetOpen] = useState(false);
  const [chatHintVisible, setChatHintVisible] = useState(false);

  // Feature 3: Food Checker State
  const [foodInputMethod, setFoodInputMethod] = useState('type'); // 'type', 'search', 'upload'
  const [ingredientsInput, setIngredientsInput] = useState('');
  const [productQuery, setProductQuery] = useState('737628064502'); // Preset barcode
  const [lookupType, setLookupType] = useState('barcode'); // 'barcode' or 'search'
  const [checkResults, setCheckResults] = useState([]);

  // Feature 4: Medication Reminders & Adherence State
  const [reminderData, setReminderData] = useState(null);
  const [patientAdherence, setPatientAdherence] = useState(null);
  const [adherenceNotes, setAdherenceNotes] = useState({});

  // Feature 5: Clinical Alerts & CDS State
  const [clinicalAlerts, setClinicalAlerts] = useState([]);
  const [patientTimeline, setPatientTimeline] = useState(null);
  const [drugFoodConflicts, setDrugFoodConflicts] = useState([]);
  const [patientAlerts, setPatientAlerts] = useState([]);
  const [activeTabStaff, setActiveTabStaff] = useState('patients'); // 'patients', 'vitals', 'alerts', 'queue', or 'referrals'
  const [isMobileNavOpen, setIsMobileNavOpen] = useState(false);
  const [activeRecordTab, setActiveRecordTab] = useState('add'); // sub-tab within a loaded patient record (doctor view) — lands on Diagnose & Prescribe, the doctor's core job
  // Lets clicking the visit-status pill (e.g. "VITALS DONE") jump straight
  // to the Vitals sub-tab below instead of making the doctor hunt for it.
  const recordSubTabsRef = useRef(null);
  // Where the "VITALS DONE" pill scrolls back to when clicked again.
  const recordTopRef = useRef(null);

  // Staff Dashboard Data State
  const [searchId, setSearchId] = useState('9001015000083');
  const [searchedPatientRecord, setSearchedPatientRecord] = useState(null);
  
  // Forms for Staff
  const [patientRegForm, setPatientRegForm] = useState({
    idNumber: '', passportNumber: '', firstName: '', lastName: '', dateOfBirth: '', gender: '', contactNumber: '', address: '', email: '',
    nextOfKinFirstName: '', nextOfKinLastName: '', nextOfKinRelationship: '', nextOfKinPhone: '',
    motherIdNumber: '', birthWeightGrams: '', birthLengthCm: '', apgarScore1Min: '', apgarScore5Min: ''
  });
  const [showRegExtras, setShowRegExtras] = useState(false);
  // Admin's registration form only: reason for visit, so a brand-new patient
  // can be checked in on the spot instead of a separate check-in step.
  const [regReasonForVisit, setRegReasonForVisit] = useState('');
  const [addDiagnosisForm, setAddDiagnosisForm] = useState({
    patientId: '', conditionName: '', notes: ''
  });
  const [addPrescriptionForm, setAddPrescriptionForm] = useState({
    patientId: '', medicationName: '', dosage: '', frequency: 'Once daily', durationDays: 7, notes: '', dispenseMethod: 'PHARMACY'
  });
  const [addAlertForm, setAddAlertForm] = useState({
    patientId: '', severity: 'HIGH', message: ''
  });

  // Nurse: Vitals capture. Primary flow is the automatic queue of patients
  // admin has checked in (waiting for vitals); ID/MRN search stays as a
  // manual fallback for walk-ins that bypassed the front desk (e.g. emergencies).
  const [vitalsSearchId, setVitalsSearchId] = useState('');
  const [vitalsPatient, setVitalsPatient] = useState(null);
  const [vitalsForm, setVitalsForm] = useState({
    bloodPressure: '', temperatureCelsius: '', pulseBpm: '', respirationRate: '', oxygenSaturation: '', weightKg: '', heightCm: '', notes: ''
  });
  const [nurseVitalsQueue, setNurseVitalsQueue] = useState([]);

  // Doctor: automatic queue of patients the nurse has just taken vitals for
  const [doctorConsultQueue, setDoctorConsultQueue] = useState([]);

  // Pharmacy: automatic queue of patients the doctor has just prescribed for.
  // ID/MRN search stays as a manual fallback.
  const [pharmacySearchId, setPharmacySearchId] = useState('9001015000083');
  const [pharmacyRecord, setPharmacyRecord] = useState(null);
  const [pharmacyQueue, setPharmacyQueue] = useState([]);

  // Nurse: paste in a lab result for the patient they've just looked up
  const [labResultForm, setLabResultForm] = useState({
    testName: '', result: '', unit: '', normalRange: '', notes: ''
  });

  // Nurse: collapsed-by-default section for registering a brand new patient
  const [showNurseRegister, setShowNurseRegister] = useState(false);

  // Admin: staff & facility management (no patient data — admin isn't clinical staff)
  const [staffList, setStaffList] = useState([]);
  const [facilityList, setFacilityList] = useState([]);
  const [adminTab, setAdminTab] = useState('frontdesk'); // 'frontdesk', 'staff', 'facilities', or 'announcements'
  const [newStaffForm, setNewStaffForm] = useState({
    staffNumber: '', firstName: '', lastName: '', role: 'NURSE', facilityId: '', email: '', password: ''
  });
  const [newFacilityForm, setNewFacilityForm] = useState({
    name: '', type: 'CLINIC', province: '', address: ''
  });

  // Admin: public announcements (rotate on the login screen)
  const [announcements, setAnnouncements] = useState([]);
  const [newAnnouncementForm, setNewAnnouncementForm] = useState({
    title: '', message: '', photoUrl: ''
  });

  // Admin: read-only referral oversight (incoming/outgoing between facilities)
  const [referralReport, setReferralReport] = useState(null);

  // Login screen: active announcements fetched without auth, rotated as a carousel
  const [publicAnnouncements, setPublicAnnouncements] = useState([]);
  const [announcementCarouselIndex, setAnnouncementCarouselIndex] = useState(0);

  // Admin front desk: register a new patient (reuses patientRegForm below) and check existing patients in
  const [checkinSearchId, setCheckinSearchId] = useState('');
  const [checkinReason, setCheckinReason] = useState('');
  const [recentCheckIns, setRecentCheckIns] = useState([]);

  // Admin front desk: look up any patient (registered any time, not just
  // today) by ID/MRN — demographics + visit history only, no clinical data.
  const [adminLookupId, setAdminLookupId] = useState('');
  const [adminLookupRecord, setAdminLookupRecord] = useState(null);

  // "Today's Queue" board — everyone still in progress at this facility
  // today, across the whole admin -> nurse -> doctor -> pharmacy pipeline.
  const [facilityQueueToday, setFacilityQueueToday] = useState([]);

  // Nurse: Lab Results, its own nav tab — look a patient up by ID/MRN and
  // attach a result, independent of whichever patient is mid-vitals.
  const [labsSearchId, setLabsSearchId] = useState('');
  const [labsPatient, setLabsPatient] = useState(null);

  // Restored feature: Referrals (Doctor/Nurse refer a patient to another facility)
  const [referralInbox, setReferralInbox] = useState([]);
  const [referralOutgoing, setReferralOutgoing] = useState([]);
  const [facilitiesForReferral, setFacilitiesForReferral] = useState([]);
  const [referralForm, setReferralForm] = useState({ toFacilityId: '', urgency: 'ROUTINE', reason: '', clinicalSummary: '' });

  // Restored feature: Immunizations (EPI schedule) — shown against a loaded patient record
  const [patientImmunizations, setPatientImmunizations] = useState([]);

  // Restored feature: Pharmacist stock/inventory tracking and a dispense event log
  const [pharmacistTab, setPharmacistTab] = useState('dispense'); // 'dispense' or 'stock'
  const [stockItems, setStockItems] = useState([]);
  const [newStockItemForm, setNewStockItemForm] = useState({ medicationName: '', unit: 'tablets', quantityOnHand: 0, reorderLevel: 10 });
  const [stockAdjustAmount, setStockAdjustAmount] = useState({});
  const [dispenseHistory, setDispenseHistory] = useState([]);
  const [dispenseLogForm, setDispenseLogForm] = useState({ prescriptionId: '', quantityDispensed: '', daysSupply: '', pharmacyNotes: '' });

  // Setup Authorization headers
  const getAuthHeaders = () => {
    return {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    };
  };

  // Log in user
  const handleLogin = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');
    setLoading(true);

    try {
      let response;
      if (loginRole === 'patient') {
        response = await fetch('/api/auth/patient/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ idNumber: patientIdNumber, dateOfBirth: patientDob })
        });
      } else {
        response = await fetch('/api/auth/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ staffNumber, password: staffPassword })
        });
      }

      const data = await response.json();
      if (!response.ok) {
        throw new Error(typeof data === 'string' ? data : data.message || 'Login failed');
      }

      // Save credentials
      localStorage.setItem('token', data.token);
      localStorage.setItem('role', data.role);
      localStorage.setItem('idNumber', data.idNumber || data.staffNumber);
      localStorage.setItem('userName', data.fullName);
      localStorage.setItem('mustChangePassword', data.mustChangePassword ? 'true' : 'false');

      setToken(data.token);
      setUserRole(data.role);
      setUserIdNumber(data.idNumber || data.staffNumber);
      setUserName(data.fullName);
      setMustChangePassword(!!data.mustChangePassword);
      setSuccessMessage('Logged in successfully!');
    } catch (err) {
      setErrorMessage(err.message);
    } finally {
      setLoading(false);
    }
  };

  // Logout
  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    localStorage.removeItem('idNumber');
    localStorage.removeItem('userName');
    localStorage.removeItem('mustChangePassword');
    setToken('');
    setUserRole('');
    setUserIdNumber('');
    setUserName('');
    setMustChangePassword(false);
    setPatientProfile(null);
    setPatientRecord(null);
    setHealthGuidance(null);
    setSearchedPatientRecord(null);
    setTriageResult(null);
    setCheckResults([]);
    setIngredientsInput('');
    setReminderData(null);
    setPatientAdherence(null);
    setClinicalAlerts([]);
    setPatientTimeline(null);
    setDrugFoodConflicts([]);
    setActiveTabStaff('patients');
    setVitalsPatient(null);
    setPharmacyRecord(null);
    setStaffList([]);
    setFacilityList([]);
    setAdminTab('frontdesk');
    setRecentCheckIns([]);
  };

  // Forced password change for a new staff account (or an admin reset)
  const handleChangePassword = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');
    if (changePasswordForm.newPassword !== changePasswordForm.confirmPassword) {
      setErrorMessage('New password and confirmation do not match.');
      return;
    }
    setLoading(true);
    try {
      const response = await fetch('/api/auth/change-password', {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({
          currentPassword: changePasswordForm.currentPassword,
          newPassword: changePasswordForm.newPassword
        })
      });
      const data = await response.json();
      if (!response.ok) throw new Error(typeof data === 'string' ? data : data.message || 'Failed to change password');

      localStorage.setItem('token', data.token);
      localStorage.setItem('mustChangePassword', 'false');
      setToken(data.token);
      setMustChangePassword(false);
      setChangePasswordForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
      setSuccessMessage('Password updated. Welcome to UDHR!');
    } catch (err) {
      setErrorMessage(err.message);
    } finally {
      setLoading(false);
    }
  };

  // Fetch data on login
  useEffect(() => {
    if (token) {
      if (userRole === 'PATIENT') {
        fetchPatientPortalData();
      } else if (userRole === 'NURSE') {
        fetchClinicalAlerts();
        fetchNurseVitalsQueue();
        // Vitals is the nurse's main job — land there instead of Register.
        setActiveTabStaff('vitals');
      } else if (userRole === 'DOCTOR') {
        fetchClinicalAlerts();
        fetchDoctorConsultQueue();
      } else if (userRole === 'PHARMACIST') {
        fetchPharmacyQueue();
      } else if (userRole === 'ADMIN') {
        fetchStaffList();
        fetchFacilityList();
        fetchRecentCheckIns();
        fetchAnnouncements();
      }
    }
  }, [token, userRole]);

  // A success banner (e.g. "Logged in successfully!") clears itself after a
  // few seconds instead of sitting there until someone dismisses it by hand.
  useEffect(() => {
    if (!successMessage) return;
    const timer = setTimeout(() => setSuccessMessage(''), 4000);
    return () => clearTimeout(timer);
  }, [successMessage]);

  // An error banner renders at the very top of the page, but the form that
  // triggered it can be scrolled well below that — jump back up so it's
  // actually seen instead of silently appearing off-screen.
  useEffect(() => {
    if (!errorMessage) return;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }, [errorMessage]);

  // Nudge patients toward the chat bubble a couple seconds after they land
  // on the portal — dismissed the moment they open the chat or close it.
  useEffect(() => {
    if (token && userRole === 'PATIENT') {
      const timer = setTimeout(() => setChatHintVisible(true), 2000);
      return () => clearTimeout(timer);
    }
  }, [token, userRole]);

  // Public announcements — no auth needed, shown on the login screen before
  // anyone signs in.
  useEffect(() => {
    fetchPublicAnnouncements();
  }, []);

  // Rotate the login-screen announcement carousel every ~4.5s when there's
  // more than one active announcement.
  useEffect(() => {
    if (publicAnnouncements.length <= 1) return;
    const interval = setInterval(() => {
      setAnnouncementCarouselIndex((i) => (i + 1) % publicAnnouncements.length);
    }, 4500);
    return () => clearInterval(interval);
  }, [publicAnnouncements.length]);

  const fetchPublicAnnouncements = async () => {
    try {
      const response = await fetch('/api/announcements/public');
      if (response.ok) {
        const data = await response.json();
        setPublicAnnouncements(data);
      }
    } catch (err) {
      // Silent — this only affects a decorative panel on the login screen.
    }
  };

  const fetchAnnouncements = async () => {
    try {
      const response = await fetch('/api/announcements', { headers: getAuthHeaders() });
      if (response.ok) {
        const data = await response.json();
        setAnnouncements(data);
      }
    } catch (err) {
      console.error('Error fetching announcements', err);
    }
  };

  const fetchReferralReport = async () => {
    try {
      const response = await fetch('/api/referrals/report', { headers: getAuthHeaders() });
      if (response.ok) {
        const data = await response.json();
        setReferralReport(data);
      }
    } catch (err) {
      console.error('Error fetching referral report', err);
    }
  };

  const handleCreateAnnouncement = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');
    setLoading(true);
    try {
      const response = await fetch('/api/announcements', {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(newAnnouncementForm)
      });
      const data = await response.json();
      if (!response.ok) throw new Error(typeof data === 'string' ? data : 'Failed to create announcement');
      setNewAnnouncementForm({ title: '', message: '', photoUrl: '' });
      setSuccessMessage('Announcement created.');
      fetchAnnouncements();
      fetchPublicAnnouncements();
    } catch (err) {
      setErrorMessage(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleToggleAnnouncement = async (id, active) => {
    setErrorMessage('');
    try {
      const response = await fetch(`/api/announcements/${id}`, {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify({ active })
      });
      if (!response.ok) throw new Error('Failed to update announcement');
      fetchAnnouncements();
      fetchPublicAnnouncements();
    } catch (err) {
      setErrorMessage(err.message);
    }
  };

  const handleDeleteAnnouncement = async (id) => {
    setErrorMessage('');
    try {
      const response = await fetch(`/api/announcements/${id}`, {
        method: 'DELETE',
        headers: getAuthHeaders()
      });
      if (!response.ok) throw new Error('Failed to delete announcement');
      fetchAnnouncements();
      fetchPublicAnnouncements();
    } catch (err) {
      setErrorMessage(err.message);
    }
  };

  const fetchPatientPortalData = async () => {
    setLoading(true);
    try {
      // 1. Fetch Profile
      const profileRes = await fetch('/api/patient/me', { headers: getAuthHeaders() });
      if (profileRes.ok) {
        const profileData = await profileRes.json();
        setPatientProfile(profileData);
      }

      // 2. Fetch Record
      const recordRes = await fetch('/api/patient/me/record', { headers: getAuthHeaders() });
      if (recordRes.ok) {
        const recordData = await recordRes.json();
        setPatientRecord(recordData);
      }

      // 3. Fetch Symptoms List
      const symptomsRes = await fetch('/api/symptoms', { headers: getAuthHeaders() });
      if (symptomsRes.ok) {
        const symptomsData = await symptomsRes.json();
        setSymptomsList(symptomsData);
      }

      // 4. Fetch Triage History
      const historyRes = await fetch('/api/symptom-checker/history', { headers: getAuthHeaders() });
      if (historyRes.ok) {
        const historyData = await historyRes.json();
        setTriageHistory(historyData);
      }

      // 5. Fetch Health Guidance & FDA warnings
      const guidanceRes = await fetch('/api/health-guidance/tips', { headers: getAuthHeaders() });
      if (guidanceRes.ok) {
        const guidanceData = await guidanceRes.json();
        setHealthGuidance(guidanceData);
      }

      // 6. Fetch Medication Reminders
      const remindersRes = await fetch('/api/reminders/patient', { headers: getAuthHeaders() });
      if (remindersRes.ok) {
        const remindersData = await remindersRes.json();
        setReminderData(remindersData);
      }

      // 7. Fetch Drug Food Audit Conflicts
      const conflictRes = await fetch('/api/clinical-alerts/drug-food-audit', { headers: getAuthHeaders() });
      if (conflictRes.ok) {
        const conflictData = await conflictRes.json();
        setDrugFoodConflicts(conflictData);
      }

      // 8. Fetch Active Clinical Alerts
      const patientAlertsRes = await fetch('/api/clinical-alerts/my-alerts', { headers: getAuthHeaders() });
      if (patientAlertsRes.ok) {
        const patientAlertsData = await patientAlertsRes.json();
        setPatientAlerts(patientAlertsData);
      }

      // 9. Fetch My Lab Results
      const myLabResultsRes = await fetch('/api/lab-results/my-results', { headers: getAuthHeaders() });
      if (myLabResultsRes.ok) {
        const myLabResultsData = await myLabResultsRes.json();
        setMyLabResults(myLabResultsData);
      }

      // 10. Fetch my active referral letter, if any
      const referralRes = await fetch('/api/patient/me/referral', { headers: getAuthHeaders() });
      if (referralRes.ok) {
        const referralData = await referralRes.json();
        setPatientReferral(referralData);
      }
    } catch (err) {
      console.error("Error fetching patient portal data", err);
    } finally {
      setLoading(false);
    }
  };

  // Fetch all unresolved alerts for doctors
  const fetchClinicalAlerts = async () => {
    try {
      const alertsRes = await fetch('/api/clinical-alerts', { headers: getAuthHeaders() });
      if (alertsRes.ok) {
        const alertsData = await alertsRes.json();
        setClinicalAlerts(alertsData);
      }
    } catch (err) {
      console.error("Error fetching clinical alerts", err);
    }
  };

  // Staff search patient. Accepts an optional idOverride so a click on the
  // automatic consultation queue can search immediately without waiting on
  // a setSearchId state update to land first.
  const handleSearchPatient = async (e, idOverride) => {
    if (e) e.preventDefault();
    const idToSearch = idOverride || searchId;
    setErrorMessage('');
    setLoading(true);
    setSearchedPatientRecord(null);
    setPatientAdherence(null);
    setPatientTimeline(null);
    if (idOverride) setSearchId(idOverride);

    try {
      const response = await fetch(`/api/patients/${idToSearch}/record`, { headers: getAuthHeaders() });
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.message || 'Patient record not found');
      }
      setSearchedPatientRecord(data);
      setAddDiagnosisForm(prev => ({ ...prev, patientId: data.patient.id }));
      setAddPrescriptionForm(prev => ({ ...prev, patientId: data.patient.id }));
      setAddAlertForm(prev => ({ ...prev, patientId: data.patient.id }));
      fetchImmunizations(data.patient.id);
      if (facilitiesForReferral.length === 0) fetchReferrals();

      // Trigger automatic CDS evaluation on patient file search to generate alerts in real time
      await fetch(`/api/clinical-alerts/patient/${data.patient.id}/evaluate`, {
        method: 'POST',
        headers: getAuthHeaders()
      });

      // Fetch Adherence Logs
      const adherenceRes = await fetch(`/api/reminders/patient/${data.patient.id}/adherence`, { headers: getAuthHeaders() });
      if (adherenceRes.ok) {
        const adherenceData = await adherenceRes.json();
        setPatientAdherence(adherenceData);
      }

      // Fetch patient timeline data (adherence logs, symptom checks, alerts, food conflicts)
      const timelineRes = await fetch(`/api/clinical-alerts/patient/${data.patient.id}`, { headers: getAuthHeaders() });
      if (timelineRes.ok) {
        const timelineData = await timelineRes.json();
        setPatientTimeline(timelineData);
      }
    } catch (err) {
      setErrorMessage(err.message);
    } finally {
      setLoading(false);
    }
  };

  // Manual Trigger to analyze patient response
  const handleEvaluatePatient = async (patientId) => {
    setErrorMessage('');
    setSuccessMessage('');
    setLoading(true);
    try {
      const response = await fetch(`/api/clinical-alerts/patient/${patientId}/evaluate`, {
        method: 'POST',
        headers: getAuthHeaders()
      });
      if (!response.ok) throw new Error('Failed to run treatment response evaluation');
      setSuccessMessage('Treatment response evaluation analyzed. Real-time CDS alerts updated.');
      
      // Refresh timeline and search details
      handleSearchPatient();
      fetchClinicalAlerts();
    } catch (err) {
      setErrorMessage(err.message);
    } finally {
      setLoading(false);
    }
  };

  // Resolve alert handler
  const handleResolveAlert = async (alertId) => {
    setErrorMessage('');
    setSuccessMessage('');
    setLoading(true);
    try {
      const response = await fetch(`/api/clinical-alerts/${alertId}/resolve`, {
        method: 'POST',
        headers: getAuthHeaders()
      });
      if (!response.ok) throw new Error('Failed to resolve clinical alert');
      setSuccessMessage('Clinical Alert marked as resolved successfully.');
      fetchClinicalAlerts();
      if (searchedPatientRecord) {
        handleSearchPatient();
      }
    } catch (err) {
      setErrorMessage(err.message);
    } finally {
      setLoading(false);
    }
  };

  // Staff registers new patient
  const handleRegisterPatient = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');

    if (patientRegForm.idNumber && patientRegForm.idNumber.length !== 13) {
      setErrorMessage('ID number must be exactly 13 digits, or left blank.');
      return;
    }

    setLoading(true);

    try {
      const response = await fetch('/api/patients', {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(patientRegForm)
      });
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.message || 'Registration failed');
      }
      let message = `Patient '${data.firstName} ${data.lastName}' registered successfully! MRN: ${data.mrn}`;

      // Admin's registration form also offers a reason for visit — if one
      // was given, check the patient straight in on the back of it instead
      // of making admin retype the same ID/MRN into a second form.
      if (regReasonForVisit.trim()) {
        try {
          const checkinResponse = await fetch('/api/checkin', {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify({ idNumber: data.idNumber || data.mrn, reason: regReasonForVisit })
          });
          const checkinData = await checkinResponse.json();
          if (!checkinResponse.ok) {
            throw new Error(checkinData.message || 'Failed to check patient in');
          }
          message += ' They have also been checked in and queued for vitals.';
          fetchRecentCheckIns();
        } catch (checkinErr) {
          message += ` (Registered, but check-in failed: ${checkinErr.message} — use "Check in patient" below.)`;
        }
      }

      setSuccessMessage(message);
      setSearchId(data.idNumber || data.mrn);
      setPatientRegForm({
        idNumber: '', passportNumber: '', firstName: '', lastName: '', dateOfBirth: '', gender: '', contactNumber: '', address: '', email: '',
        nextOfKinFirstName: '', nextOfKinLastName: '', nextOfKinRelationship: '', nextOfKinPhone: '',
        motherIdNumber: '', birthWeightGrams: '', birthLengthCm: '', apgarScore1Min: '', apgarScore5Min: ''
      });
      setRegReasonForVisit('');
      setShowRegExtras(false);
      // Load the newly registered patient record
      setSearchedPatientRecord({
        patient: data, allergies: [], chronicConditions: [], visits: [], diagnoses: [], prescriptions: [], labResults: []
      });
    } catch (err) {
      setErrorMessage(err.message);
    } finally {
      setLoading(false);
    }
  };

  // Staff adds a diagnosis
  const handleAddDiagnosis = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');

    try {
      const payload = {
        patientId: addDiagnosisForm.patientId,
        diagnosis: addDiagnosisForm.conditionName,
        notes: addDiagnosisForm.notes
      };
      const response = await fetch('/api/diagnoses', {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(payload)
      });
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.message || 'Failed to add diagnosis');
      }
      setSuccessMessage('Diagnosis added successfully!');
      setAddDiagnosisForm(prev => ({ ...prev, conditionName: '', notes: '' }));
      handleSearchPatient(); // Refresh record
      fetchDoctorConsultQueue();
    } catch (err) {
      setErrorMessage(err.message);
    }
  };

  // Staff adds a prescription
  const handleAddPrescription = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');

    try {
      const response = await fetch('/api/prescriptions', {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(addPrescriptionForm)
      });
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.message || 'Failed to add prescription');
      }
      setSuccessMessage(
        addPrescriptionForm.dispenseMethod === 'SELF'
          ? 'Prescription saved and marked as dispensed to the patient.'
          : "Sent to the pharmacy. You stay on this patient's record in case you need to add another diagnosis or prescription — click \"← Back to queue\" above when you're done."
      );
      setAddPrescriptionForm(prev => ({ ...prev, medicationName: '', dosage: '', frequency: 'Once daily', durationDays: 7, notes: '', dispenseMethod: 'PHARMACY' }));
      handleSearchPatient(); // Refresh record
    } catch (err) {
      setErrorMessage(err.message);
    }
  };

  // Nurse looks a patient up by ID number before capturing vitals — no queue ticket needed
  const handleLookupForVitals = async (e) => {
    if (e) e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');
    setVitalsPatient(null);
    setLoading(true);
    try {
      const response = await fetch(`/api/patients/${vitalsSearchId}`, { headers: getAuthHeaders() });
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.message || 'Patient not found');
      }
      setVitalsPatient(data);
    } catch (err) {
      setErrorMessage(err.message);
    } finally {
      setLoading(false);
    }
  };

  // Nurse records vitals; this automatically hands the patient off to the doctor
  const handleRecordVitals = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');
    setLoading(true);
    try {
      const payload = {
        idNumber: vitalsSearchId,
        bloodPressure: vitalsForm.bloodPressure,
        temperatureCelsius: vitalsForm.temperatureCelsius ? parseFloat(vitalsForm.temperatureCelsius) : null,
        pulseBpm: vitalsForm.pulseBpm ? parseInt(vitalsForm.pulseBpm) : null,
        respirationRate: vitalsForm.respirationRate ? parseInt(vitalsForm.respirationRate) : null,
        oxygenSaturation: vitalsForm.oxygenSaturation ? parseInt(vitalsForm.oxygenSaturation) : null,
        weightKg: vitalsForm.weightKg ? parseFloat(vitalsForm.weightKg) : null,
        heightCm: vitalsForm.heightCm ? parseFloat(vitalsForm.heightCm) : null,
        notes: vitalsForm.notes
      };
      const response = await fetch('/api/vitals', {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(payload)
      });
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.message || 'Failed to record vitals');
      }
      setSuccessMessage(`Vitals recorded for ${vitalsPatient.firstName} ${vitalsPatient.lastName}. They now appear automatically on the doctor's queue.`);
      setVitalsForm({ bloodPressure: '', temperatureCelsius: '', pulseBpm: '', respirationRate: '', oxygenSaturation: '', weightKg: '', heightCm: '', notes: '' });
      setVitalsPatient(null);
      setVitalsSearchId('');
      fetchNurseVitalsQueue();
    } catch (err) {
      setErrorMessage(err.message);
    } finally {
      setLoading(false);
    }
  };

  // Pharmacist looks a patient up by ID and sees exactly where they came
  // from. Accepts an optional idOverride for a click straight off the
  // automatic dispense queue.
  const handleSearchPharmacyPatient = async (e, idOverride) => {
    if (e) e.preventDefault();
    const idToSearch = idOverride || pharmacySearchId;
    setErrorMessage('');
    setSuccessMessage('');
    setPharmacyRecord(null);
    setLoading(true);
    if (idOverride) setPharmacySearchId(idOverride);
    try {
      const response = await fetch(`/api/pharmacy/patient/${idToSearch}`, { headers: getAuthHeaders() });
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.message || 'Patient not found');
      }
      setPharmacyRecord(data);
      fetchDispenseHistoryForPatient(data.patient.id);
    } catch (err) {
      setErrorMessage(err.message);
    } finally {
      setLoading(false);
    }
  };

  // Pharmacist dispenses one pending prescription
  const handleDispense = async (prescriptionId) => {
    setErrorMessage('');
    setSuccessMessage('');
    try {
      const response = await fetch(`/api/pharmacy/dispense/${prescriptionId}`, {
        method: 'POST',
        headers: getAuthHeaders()
      });
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.message || 'Failed to dispense prescription');
      }
      setSuccessMessage('Marked as dispensed.');
      handleSearchPharmacyPatient();
      fetchPharmacyQueue();
    } catch (err) {
      setErrorMessage(err.message);
    }
  };

  // Nurse pastes a lab result in for the patient looked up on the Lab Results tab
  const handleAddLabResult = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');
    try {
      const payload = { idNumber: labsSearchId, ...labResultForm };
      const response = await fetch('/api/lab-results', {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(payload)
      });
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.message || 'Failed to save lab result');
      }
      setSuccessMessage('Lab result saved to the patient\'s file.');
      setLabResultForm({ testName: '', result: '', unit: '', normalRange: '', notes: '' });
    } catch (err) {
      setErrorMessage(err.message);
    }
  };

  // Admin: staff & facility management
  const fetchStaffList = async () => {
    setErrorMessage('');
    setLoading(true);
    try {
      const response = await fetch('/api/staff', { headers: getAuthHeaders() });
      const data = await response.json();
      if (!response.ok) throw new Error(data.message || 'Failed to load staff');
      setStaffList(data);
    } catch (err) {
      setErrorMessage(err.message);
    } finally {
      setLoading(false);
    }
  };

  const fetchFacilityList = async () => {
    setErrorMessage('');
    setLoading(true);
    try {
      const response = await fetch('/api/facilities', { headers: getAuthHeaders() });
      const data = await response.json();
      if (!response.ok) throw new Error(data.message || 'Failed to load facilities');
      setFacilityList(data);
    } catch (err) {
      setErrorMessage(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleRegisterStaff = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');
    try {
      const response = await fetch('/api/staff', {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(newStaffForm)
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.message || 'Failed to register staff member');
      setSuccessMessage(`Staff member '${data.firstName} ${data.lastName}' (${data.role}) created.`);
      setNewStaffForm({ staffNumber: '', firstName: '', lastName: '', role: 'NURSE', facilityId: newStaffForm.facilityId, email: '', password: '' });
      fetchStaffList();
    } catch (err) {
      setErrorMessage(err.message);
    }
  };

  const handleDeactivateStaff = async (staffId) => {
    setErrorMessage('');
    setSuccessMessage('');
    try {
      const response = await fetch(`/api/staff/${staffId}/deactivate`, {
        method: 'PUT',
        headers: getAuthHeaders()
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.message || 'Failed to deactivate staff member');
      setSuccessMessage('Staff member deactivated.');
      fetchStaffList();
    } catch (err) {
      setErrorMessage(err.message);
    }
  };

  const handleAddFacility = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');
    try {
      const response = await fetch('/api/facilities', {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(newFacilityForm)
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.message || 'Failed to add facility');
      setSuccessMessage(`Facility '${data.name}' added.`);
      setNewFacilityForm({ name: '', type: 'CLINIC', province: '', address: '' });
      fetchFacilityList();
    } catch (err) {
      setErrorMessage(err.message);
    }
  };

  // Front desk: log that a patient has arrived and puts them straight onto
  // the nurse's automatic vitals queue. This is also admin's record of who
  // has come in today and whether they're a new or returning patient.
  const fetchRecentCheckIns = async () => {
    setErrorMessage('');
    try {
      const response = await fetch('/api/checkin/recent', { headers: getAuthHeaders() });
      const data = await response.json();
      if (!response.ok) throw new Error(data.message || 'Failed to load check-ins');
      setRecentCheckIns(data);
    } catch (err) {
      setErrorMessage(err.message);
    }
  };

  const handleCheckIn = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');
    try {
      const response = await fetch('/api/checkin', {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({ idNumber: checkinSearchId, reason: checkinReason })
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.message || 'Failed to check patient in');
      setSuccessMessage(`Checked in ${data.patient.firstName} ${data.patient.lastName}. They now appear automatically on the nurse's vitals queue.`);
      setCheckinSearchId('');
      setCheckinReason('');
      fetchRecentCheckIns();
    } catch (err) {
      setErrorMessage(err.message);
    }
  };

  // Admin looks up a patient who isn't on today's list — e.g. confirming
  // they're already registered, or checking when they were last seen.
  const handleAdminPatientLookup = async (e) => {
    if (e) e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');
    setAdminLookupRecord(null);
    setLoading(true);
    try {
      const response = await fetch(`/api/checkin/lookup/${adminLookupId}`, { headers: getAuthHeaders() });
      const data = await parseResponseBody(response);
      if (!response.ok) throw new Error(data.message || 'Patient not found');
      setAdminLookupRecord(data);
    } catch (err) {
      setErrorMessage(err.message);
    } finally {
      setLoading(false);
    }
  };

  // Staff adds a custom clinical alert
  const handleAddAlert = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');
    setLoading(true);

    try {
      const response = await fetch('/api/clinical-alerts', {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({
          patientId: addAlertForm.patientId,
          severity: addAlertForm.severity,
          message: addAlertForm.message
        })
      });
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.message || 'Failed to add custom alert');
      }
      setSuccessMessage('Custom Clinical Alert created successfully!');
      setAddAlertForm(prev => ({ ...prev, message: '' }));
      fetchClinicalAlerts();
      handleSearchPatient(); // Refresh record
    } catch (err) {
      setErrorMessage(err.message);
    } finally {
      setLoading(false);
    }
  };

  // Patient symptom check submission
  const handleSymptomCheckSubmit = async (e) => {
    e.preventDefault();
    if (selectedSymptoms.length === 0) {
      setErrorMessage('Please select at least one symptom');
      return;
    }
    setErrorMessage('');
    setLoading(true);
    setTriageResult(null);

    try {
      const response = await fetch('/api/symptom-checker/check', {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(selectedSymptoms)
      });
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.message || 'Symptom check triage failed');
      }
      setTriageResult(data);
      setSelectedSymptoms([]);
      // Reload history and guidelines
      fetchPatientPortalData();
    } catch (err) {
      setErrorMessage(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleSendChatMessage = async (e) => {
    e.preventDefault();
    const text = chatInput.trim();
    if (!text || chatSending) return;

    setChatMessages((prev) => [...prev, { sender: 'user', text }]);
    setChatInput('');
    setChatSending(true);

    try {
      const response = await fetch('/api/patient/chatbot', {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({ message: text })
      });
      const data = await response.json();
      if (!response.ok) {
        throw new Error(typeof data === 'string' ? data : 'The health assistant could not respond right now.');
      }
      setChatMessages((prev) => [...prev, { sender: 'bot', text: data.reply, urgencyLevel: data.urgencyLevel }]);
      if (data.urgencyLevel) {
        // A symptom check was just performed under the hood — refresh history.
        fetchPatientPortalData();
      }
    } catch (err) {
      setChatMessages((prev) => [...prev, { sender: 'bot', text: err.message || 'Something went wrong. Please try again.' }]);
    } finally {
      setChatSending(false);
    }
  };

  const handleSymptomToggle = (id) => {
    if (selectedSymptoms.includes(id)) {
      setSelectedSymptoms(selectedSymptoms.filter(sId => sId !== id));
    } else {
      setSelectedSymptoms([...selectedSymptoms, id]);
    }
  };

  // Update Medication Adherence (Patient Portal)
  const handleUpdateAdherence = async (adherenceId, status, notes = '') => {
    setErrorMessage('');
    setSuccessMessage('');
    setLoading(true);
    try {
      const response = await fetch(`/api/reminders/adherence/${adherenceId}`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({ status, notes })
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.message || 'Failed to update adherence status');
      setSuccessMessage(`Medication marked as ${status.toLowerCase()}!`);
      setAdherenceNotes(prev => ({ ...prev, [adherenceId]: '' }));
      fetchPatientPortalData(); // Reload reminders & statistics
    } catch (err) {
      setErrorMessage(err.message);
    } finally {
      setLoading(false);
    }
  };

  // Feature 3: OCR File Upload Handler
  const handleOcrUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    setErrorMessage('');
    setSuccessMessage('');
    setLoading(true);
    
    const formData = new FormData();
    formData.append('file', file);
    
    try {
      const response = await fetch('/api/food-checker/ocr', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`
        },
        body: formData
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.message || 'OCR extraction failed');
      setIngredientsInput(data.extractedText);
      setSuccessMessage(`Ingredients read from '${file.name}' successfully!`);
    } catch (err) {
      setErrorMessage(err.message);
    } finally {
      setLoading(false);
    }
  };

  // Feature 3: Open Food Facts Lookup
  const handleProductLookup = async (e) => {
    e.preventDefault();
    if (!productQuery) return;
    setErrorMessage('');
    setSuccessMessage('');
    setLoading(true);
    
    try {
      const response = await fetch(`/api/food-checker/search?type=${lookupType}&query=${productQuery}`, {
        headers: getAuthHeaders()
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.message || 'Product ingredients search failed');
      setIngredientsInput(data.ingredients);
      setSuccessMessage('Ingredients fetched from Open Food Facts!');
    } catch (err) {
      setErrorMessage(err.message);
    } finally {
      setLoading(false);
    }
  };

  // Feature 3: Ingredient cross-reference check
  const handleCheckIngredients = async (e) => {
    e.preventDefault();
    if (!ingredientsInput) {
      setErrorMessage('Please type or fetch ingredients first');
      return;
    }
    setErrorMessage('');
    setSuccessMessage('');
    setLoading(true);
    setCheckResults([]);
    
    try {
      const response = await fetch('/api/food-checker/check', {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({ ingredients: ingredientsInput })
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.message || 'Analysis failed');
      setCheckResults(data);
      setSuccessMessage('Ingredients cross-checked against your medical record!');
      fetchPatientPortalData(); // Refresh history and conflict warnings
    } catch (err) {
      setErrorMessage(err.message);
    } finally {
      setLoading(false);
    }
  };

  // Some restored-feature endpoints return a plain-text error body instead of JSON;
  // this reads either shape without throwing a JSON parse error.
  const parseResponseBody = async (response) => {
    const text = await response.text();
    try {
      return JSON.parse(text);
    } catch {
      return { message: text };
    }
  };

  // ===== Automatic hand-off queues: admin -> nurse -> doctor -> pharmacy =====
  // Each stage reads a list of patients the previous stage already queued —
  // nobody has to search for a patient by ID except as a manual fallback.
  const fetchNurseVitalsQueue = async () => {
    try {
      const response = await fetch('/api/visits/queue/WAITING_VITALS', { headers: getAuthHeaders() });
      const data = await parseResponseBody(response);
      if (!response.ok) throw new Error(data.message || 'Failed to load the vitals queue');
      setNurseVitalsQueue(data);
    } catch (err) {
      setErrorMessage(err.message);
    }
  };

  const fetchDoctorConsultQueue = async () => {
    try {
      const response = await fetch('/api/visits/queue/VITALS_DONE', { headers: getAuthHeaders() });
      const data = await parseResponseBody(response);
      if (!response.ok) throw new Error(data.message || 'Failed to load the consultation queue');
      setDoctorConsultQueue(data);
    } catch (err) {
      setErrorMessage(err.message);
    }
  };

  const fetchPharmacyQueue = async () => {
    try {
      const response = await fetch('/api/pharmacy/queue', { headers: getAuthHeaders() });
      const data = await parseResponseBody(response);
      if (!response.ok) throw new Error(data.message || 'Failed to load the pharmacy queue');
      setPharmacyQueue(data);
    } catch (err) {
      setErrorMessage(err.message);
    }
  };

  // Nurse picks a patient straight off the vitals queue — same effect as
  // looking them up by ID, just without typing anything.
  const handleSelectFromVitalsQueue = (visit) => {
    setErrorMessage('');
    setSuccessMessage('');
    setVitalsSearchId(visit.patient.idNumber || visit.patient.mrn);
    setVitalsPatient(visit.patient);
  };

  // Doctor picks a patient straight off the consultation queue — loads the
  // full record exactly like a manual search would.
  const handleSelectFromDoctorQueue = (visit) => {
    handleSearchPatient(null, visit.patient.idNumber || visit.patient.mrn);
  };

  // Pharmacist picks a patient straight off the dispense queue.
  const handleSelectFromPharmacyQueue = (item) => {
    handleSearchPharmacyPatient(null, item.patient.idNumber || item.patient.mrn);
  };

  // "Back" out of a selected patient and return to the queue list — each
  // role's detail view replaces the list rather than stacking below it, so
  // this is how you get back to picking someone else.
  const handleBackToVitalsQueue = () => {
    setVitalsPatient(null);
    setVitalsSearchId('');
  };

  const handleBackToDoctorQueue = () => {
    setSearchedPatientRecord(null);
    setPatientAdherence(null);
    setPatientTimeline(null);
    fetchDoctorConsultQueue();
  };

  const handleBackToPharmacyQueue = () => {
    setPharmacyRecord(null);
    fetchPharmacyQueue();
  };

  // "Today's Queue" — a live, read-only board of every patient still in
  // progress at this facility today, across the whole pipeline.
  const fetchFacilityQueueToday = async () => {
    try {
      const response = await fetch('/api/visits/today', { headers: getAuthHeaders() });
      const data = await parseResponseBody(response);
      if (!response.ok) throw new Error(data.message || "Failed to load today's queue");
      setFacilityQueueToday(data);
    } catch (err) {
      setErrorMessage(err.message);
    }
  };

  // Nurse looks a patient up by ID/MRN to attach a lab result — independent
  // of whichever patient they're currently taking vitals for.
  const handleLookupForLabs = async (e) => {
    if (e) e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');
    setLabsPatient(null);
    setLoading(true);
    try {
      const response = await fetch(`/api/patients/${labsSearchId}`, { headers: getAuthHeaders() });
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.message || 'Patient not found');
      }
      setLabsPatient(data);
    } catch (err) {
      setErrorMessage(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleBackToLabsSearch = () => {
    setLabsPatient(null);
    setLabsSearchId('');
  };

  // ===== Restored feature: Referrals =====
  const fetchReferrals = async () => {
    try {
      const [inRes, outRes, facRes] = await Promise.all([
        fetch('/api/referrals/incoming', { headers: getAuthHeaders() }),
        fetch('/api/referrals/outgoing', { headers: getAuthHeaders() }),
        fetch('/api/facilities', { headers: getAuthHeaders() })
      ]);
      const inData = await parseResponseBody(inRes);
      const outData = await parseResponseBody(outRes);
      if (inRes.ok) setReferralInbox(inData);
      if (outRes.ok) setReferralOutgoing(outData);
      if (facRes.ok) setFacilitiesForReferral(await parseResponseBody(facRes));
    } catch (err) {
      setErrorMessage(err.message);
    }
  };

  const handleCreateReferral = async (e) => {
    e.preventDefault();
    if (!searchedPatientRecord) {
      setErrorMessage('Locate a patient first, then refer them to another facility.');
      return;
    }
    setErrorMessage('');
    setSuccessMessage('');
    setLoading(true);
    try {
      const response = await fetch('/api/referrals', {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({ patientId: searchedPatientRecord.patient.id, ...referralForm })
      });
      const data = await parseResponseBody(response);
      if (!response.ok) throw new Error(data.message || 'Referral failed');
      setSuccessMessage('Referral sent.');
      setReferralForm({ toFacilityId: '', urgency: 'ROUTINE', reason: '', clinicalSummary: '' });
      fetchReferrals();
    } catch (err) {
      setErrorMessage(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleRespondReferral = async (id, status) => {
    setErrorMessage('');
    setSuccessMessage('');
    try {
      const response = await fetch(`/api/referrals/${id}/respond`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({ status })
      });
      const data = await parseResponseBody(response);
      if (!response.ok) throw new Error(data.message || 'Failed to respond to referral');
      setSuccessMessage(`Referral marked ${status.toLowerCase()}.`);
      fetchReferrals();
    } catch (err) {
      setErrorMessage(err.message);
    }
  };

  // ===== Restored feature: Immunizations (EPI schedule) =====
  const fetchImmunizations = async (patientId) => {
    try {
      const response = await fetch(`/api/immunizations/patient/${patientId}`, { headers: getAuthHeaders() });
      const data = await parseResponseBody(response);
      if (response.ok) setPatientImmunizations(data);
    } catch (err) {
      setErrorMessage(err.message);
    }
  };

  const handleGenerateImmunizationSchedule = async (patientId) => {
    setErrorMessage('');
    setSuccessMessage('');
    try {
      const response = await fetch(`/api/immunizations/patient/${patientId}/schedule`, { method: 'POST', headers: getAuthHeaders() });
      const data = await parseResponseBody(response);
      if (!response.ok) throw new Error(data.message || 'Failed to generate immunization schedule');
      setPatientImmunizations(data);
      setSuccessMessage('EPI immunization schedule generated.');
    } catch (err) {
      setErrorMessage(err.message);
    }
  };

  const handleImmunizationAction = async (id, action) => {
    setErrorMessage('');
    setSuccessMessage('');
    try {
      const options = { method: 'POST', headers: getAuthHeaders() };
      if (action === 'administer') options.body = JSON.stringify({});
      const response = await fetch(`/api/immunizations/${id}/${action}`, options);
      const data = await parseResponseBody(response);
      if (!response.ok) throw new Error(data.message || `Failed to update immunization`);
      if (searchedPatientRecord) fetchImmunizations(searchedPatientRecord.patient.id);
    } catch (err) {
      setErrorMessage(err.message);
    }
  };

  // ===== Restored feature: Pharmacy stock/inventory =====
  const fetchStockItems = async () => {
    try {
      const response = await fetch('/api/stock', { headers: getAuthHeaders() });
      const data = await parseResponseBody(response);
      if (response.ok) setStockItems(data);
    } catch (err) {
      setErrorMessage(err.message);
    }
  };

  const handleAddStockItem = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');
    try {
      const response = await fetch('/api/stock', {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(newStockItemForm)
      });
      const data = await parseResponseBody(response);
      if (!response.ok) throw new Error(data.message || 'Failed to add stock item');
      setSuccessMessage(`${data.medicationName} added to stock.`);
      setNewStockItemForm({ medicationName: '', unit: 'tablets', quantityOnHand: 0, reorderLevel: 10 });
      fetchStockItems();
    } catch (err) {
      setErrorMessage(err.message);
    }
  };

  const handleAdjustStock = async (stockItemId) => {
    const quantityChange = parseInt(stockAdjustAmount[stockItemId], 10);
    if (!quantityChange) return;
    setErrorMessage('');
    setSuccessMessage('');
    try {
      const response = await fetch('/api/stock/adjust', {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({ stockItemId, quantityChange, notes: 'Manual adjustment' })
      });
      const data = await parseResponseBody(response);
      if (!response.ok) throw new Error(data.message || 'Failed to adjust stock');
      setStockAdjustAmount(prev => ({ ...prev, [stockItemId]: '' }));
      fetchStockItems();
    } catch (err) {
      setErrorMessage(err.message);
    }
  };

  // ===== Restored feature: Dispense event log =====
  const fetchDispenseHistoryForPatient = async (patientId) => {
    try {
      const response = await fetch(`/api/dispensing/patient/${patientId}`, { headers: getAuthHeaders() });
      const data = await parseResponseBody(response);
      if (response.ok) setDispenseHistory(data);
    } catch (err) {
      setErrorMessage(err.message);
    }
  };

  const handleLogDispense = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');
    try {
      const response = await fetch('/api/dispensing', {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(dispenseLogForm)
      });
      const data = await parseResponseBody(response);
      if (!response.ok) throw new Error(data.message || 'Failed to log dispense');
      setSuccessMessage('Dispense event logged and stock updated.');
      setDispenseLogForm({ prescriptionId: '', quantityDispensed: '', daysSupply: '', pharmacyNotes: '' });
      if (pharmacyRecord) fetchDispenseHistoryForPatient(pharmacyRecord.patient.id);
    } catch (err) {
      setErrorMessage(err.message);
    }
  };

  // Role helpers — each staff role gets a purpose-built screen instead of one shared dashboard
  const isNurse = userRole === 'NURSE';
  const isPharmacist = userRole === 'PHARMACIST';
  const isDoctor = userRole === 'DOCTOR';
  const isAdmin = userRole === 'ADMIN';

  // 1. Login — light clinical redesign. Rendered on its own, ahead of the
  // shared app shell below, since it uses a full-viewport two-pane layout
  // rather than the header + centered-card layout the other screens still use.
  if (!token) {
    return (
      <div className="udhr-login-shell">
        <div className="udhr-login-brand">
          <div className="udhr-login-logo">
            <div className="udhr-login-logo-chip">
              <Activity size={20} color="#fff" />
            </div>
            <span style={{ fontWeight: 700, fontSize: '18px', letterSpacing: '-0.01em' }}>UDHR</span>
          </div>
          <div>
            <h1 className="udhr-login-headline">Your health record,<br />in one place.</h1>
            <p className="udhr-login-tagline">
              Universal Digital Health Record connects patients, clinics and pharmacies so care follows you — not your paperwork.
            </p>
          </div>

          {publicAnnouncements.length > 0 && (() => {
            const current = publicAnnouncements[announcementCarouselIndex % publicAnnouncements.length];
            return (
              <div className="udhr-announcement-card">
                <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
                  {current.photoUrl && (
                    <div className="udhr-announcement-photo">
                      <img src={current.photoUrl} alt="" onError={(e) => { e.target.parentElement.style.display = 'none'; }} />
                    </div>
                  )}
                  <div style={{ minWidth: 0 }}>
                    <p className="udhr-announcement-label">Public Notice</p>
                    <p className="udhr-announcement-title">{current.title}</p>
                    <p className="udhr-announcement-message">{current.message}</p>
                  </div>
                </div>
                {publicAnnouncements.length > 1 && (
                  <div className="udhr-announcement-dots">
                    {publicAnnouncements.map((_, i) => (
                      <button
                        key={i}
                        type="button"
                        className={`udhr-announcement-dot ${i === announcementCarouselIndex % publicAnnouncements.length ? 'active' : ''}`}
                        onClick={() => setAnnouncementCarouselIndex(i)}
                        aria-label={`Show announcement ${i + 1}`}
                      />
                    ))}
                  </div>
                )}
              </div>
            );
          })()}

          <p className="udhr-login-copyright">© 2026 UDHR · National Health Network</p>
        </div>

        <div className="udhr-login-form-pane">
          <div className="udhr-login-form-col">
            <h2 className="udhr-login-title">Sign in</h2>
            <p className="udhr-login-subtitle">Choose how you're accessing UDHR.</p>

            {errorMessage && (
              <div className="udhr-alert-banner error">
                <AlertCircle size={16} />
                <span style={{ flex: 1 }}>{errorMessage}</span>
                <button type="button" onClick={() => setErrorMessage('')}>×</button>
              </div>
            )}
            {successMessage && (
              <div className="udhr-alert-banner success">
                <CheckCircle size={16} />
                <span style={{ flex: 1 }}>{successMessage}</span>
                <button type="button" onClick={() => setSuccessMessage('')}>×</button>
              </div>
            )}

            <div className="udhr-segmented">
              <button
                type="button"
                className={`udhr-segmented-btn ${loginRole === 'patient' ? 'active' : ''}`}
                onClick={() => { setLoginRole('patient'); setErrorMessage(''); }}
              >
                Patient
              </button>
              <button
                type="button"
                className={`udhr-segmented-btn ${loginRole === 'staff' ? 'active' : ''}`}
                onClick={() => { setLoginRole('staff'); setErrorMessage(''); }}
              >
                Healthcare Staff
              </button>
            </div>

            <form onSubmit={handleLogin}>
              {loginRole === 'patient' ? (
                <>
                  <div className="udhr-form-group">
                    <label className="udhr-label" htmlFor="patientId">South African ID Number</label>
                    <input
                      type="text"
                      id="patientId"
                      className="udhr-input"
                      value={patientIdNumber}
                      onChange={(e) => setPatientIdNumber(e.target.value)}
                      placeholder="e.g. 9001015000083"
                      required
                    />
                  </div>
                  <div className="udhr-form-group">
                    <label className="udhr-label" htmlFor="patientDob">Date of Birth</label>
                    <input
                      type="date"
                      id="patientDob"
                      className="udhr-input"
                      value={patientDob}
                      onChange={(e) => setPatientDob(e.target.value)}
                      required
                    />
                  </div>
                </>
              ) : (
                <>
                  <div className="udhr-form-group">
                    <label className="udhr-label" htmlFor="staffNum">Staff Number</label>
                    <input
                      type="text"
                      id="staffNum"
                      className="udhr-input"
                      value={staffNumber}
                      onChange={(e) => setStaffNumber(e.target.value)}
                      placeholder="e.g. DOC001 or NUR001"
                      required
                    />
                  </div>
                  <div className="udhr-form-group">
                    <label className="udhr-label" htmlFor="staffPass">Password</label>
                    <div className="udhr-password-field">
                      <input
                        type={showStaffPassword ? 'text' : 'password'}
                        id="staffPass"
                        className="udhr-input"
                        value={staffPassword}
                        onChange={(e) => setStaffPassword(e.target.value)}
                        placeholder="••••••••"
                        required
                      />
                      <button type="button" className="udhr-password-toggle" onClick={() => setShowStaffPassword(!showStaffPassword)} aria-label={showStaffPassword ? 'Hide password' : 'Show password'}>
                        {showStaffPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                      </button>
                    </div>
                  </div>
                </>
              )}

              <button type="submit" className="udhr-btn-primary" disabled={loading}>
                {loading ? 'Signing in...' : 'Sign In'}
              </button>
            </form>
          </div>
        </div>
      </div>
    );
  }

  // 2. Patient Portal — light clinical redesign. Single-column, tabbed layout
  // replacing the old one-long-scroll dashboard. Reuses every existing
  // patient-portal state value and handler as-is; only presentation and
  // navigation (tabs vs. scroll) changed.
  if (token && userRole === 'PATIENT' && !mustChangePassword) {
    const hasUrgentFlag = (patientAlerts && patientAlerts.length > 0) || (drugFoodConflicts && drugFoodConflicts.length > 0);
    const urgentFlagText = drugFoodConflicts && drugFoodConflicts.length > 0
      ? drugFoodConflicts[0].message
      : (patientAlerts && patientAlerts[0]?.message);

    return (
      <div className="udhr-page">
        <div className="udhr-page-inner">
          <header className="udhr-page-header">
            <div className="udhr-page-logo">
              <div className="udhr-page-logo-chip">
                <Activity size={16} color="#fff" />
              </div>
              UDHR
            </div>
            <button className="udhr-logout-link" onClick={handleLogout}>
              <LogOut size={15} /> Log out
            </button>
          </header>

          <div className="udhr-greeting">
            <h1>Hi, {patientProfile?.firstName || 'there'}</h1>
            <p>Here's where things stand today.</p>
          </div>

          {errorMessage && (
            <div className="udhr-alert-banner error" style={{ marginTop: '16px' }}>
              <AlertCircle size={16} />
              <span style={{ flex: 1 }}>{errorMessage}</span>
              <button type="button" onClick={() => setErrorMessage('')}>×</button>
            </div>
          )}
          {successMessage && (
            <div className="udhr-alert-banner success" style={{ marginTop: '16px' }}>
              <CheckCircle size={16} />
              <span style={{ flex: 1 }}>{successMessage}</span>
              <button type="button" onClick={() => setSuccessMessage('')}>×</button>
            </div>
          )}

          {hasUrgentFlag && urgentFlagText && (
            <div className="udhr-alert-banner error" style={{ marginTop: '16px', alignItems: 'flex-start' }}>
              <AlertTriangle size={18} style={{ flexShrink: 0, marginTop: '1px' }} />
              <div>
                <p style={{ margin: 0, fontWeight: 600 }}>Possible interaction flagged</p>
                <p style={{ margin: '2px 0 0' }}>{urgentFlagText}</p>
              </div>
            </div>
          )}

          <div className="udhr-tab-bar" style={{ marginTop: '20px' }}>
            {[
              ['overview', 'Overview'],
              ['symptoms', 'Symptom Checker'],
              ['medications', 'Medications'],
              ['food', 'Food Checker'],
              ['labs', 'Lab Results'],
              ['referral', 'Referral Letter'],
            ].map(([key, label]) => (
              <button
                key={key}
                type="button"
                className={`udhr-tab-pill ${patientPortalTab === key ? 'active' : ''}`}
                onClick={() => setPatientPortalTab(key)}
              >
                {label}
              </button>
            ))}
          </div>

          {/* ===== Overview ===== */}
          {patientPortalTab === 'overview' && (
            <>
              <div className="udhr-stat-grid">
                <div className="udhr-stat-card">
                  <p className="udhr-stat-label">Next dose</p>
                  <p className="udhr-stat-value">
                    {(() => {
                      const pending = (reminderData?.adherenceLogs || [])
                        .filter(l => l.status === 'PENDING')
                        .sort((a, b) => new Date(a.scheduledTime) - new Date(b.scheduledTime));
                      if (pending.length === 0) return '—';
                      const next = pending[0];
                      return `${new Date(next.scheduledTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} · ${next.reminder.prescription.medication}`;
                    })()}
                  </p>
                </div>
                <div className="udhr-stat-card">
                  <p className="udhr-stat-label">Weekly adherence</p>
                  <p className="udhr-stat-value">
                    {reminderData?.stats?.totalDoses > 0 ? `${reminderData.stats.adherenceScore}%` : '—'}
                  </p>
                </div>
                <div className="udhr-stat-card">
                  <p className="udhr-stat-label">Last triage</p>
                  <p className="udhr-stat-value" style={{
                    color: triageHistory[0]?.urgencyLevel === 'RED' ? 'var(--udhr-danger)' : triageHistory[0]?.urgencyLevel === 'YELLOW' ? 'var(--udhr-warning)' : undefined
                  }}>
                    {triageHistory[0]?.urgencyLevel || '—'}
                  </p>
                </div>
                <div className="udhr-stat-card">
                  <p className="udhr-stat-label">Active alerts</p>
                  <p className="udhr-stat-value" style={{ color: patientAlerts.length > 0 ? 'var(--udhr-danger)' : undefined }}>
                    {patientAlerts.length}
                  </p>
                </div>
              </div>

              {(healthGuidance?.conditions?.length > 0 || healthGuidance?.allergies?.length > 0) && (
                <>
                  <h2 className="udhr-section-title">Your health profile</h2>
                  <div className="udhr-chip-row">
                    {healthGuidance?.conditions.map((c, i) => (
                      <span key={`c-${i}`} className="udhr-tag info">Condition: {c}</span>
                    ))}
                    {healthGuidance?.allergies.map((a, i) => (
                      <span key={`a-${i}`} className="udhr-tag danger">⚠️ Allergy: {a}</span>
                    ))}
                  </div>
                </>
              )}

              <h2 className="udhr-section-title">Quick actions</h2>
              <div className="udhr-quick-grid">
                <button type="button" className="udhr-quick-card" onClick={() => setChatWidgetOpen(true)}>
                  <MessageCircle size={20} color="#2563eb" />
                  <span>Ask the health assistant</span>
                </button>
                <button type="button" className="udhr-quick-card" onClick={() => setPatientPortalTab('symptoms')}>
                  <Search size={20} color="#2563eb" />
                  <span>Check my symptoms</span>
                </button>
                <button type="button" className="udhr-quick-card" onClick={() => setPatientPortalTab('food')}>
                  <FileSpreadsheet size={20} color="#2563eb" />
                  <span>Check food ingredients</span>
                </button>
              </div>

              {patientAlerts && patientAlerts.length > 0 && (
                <>
                  <h2 className="udhr-section-title">Clinical warnings & doctor's instructions</h2>
                  {patientAlerts.map((alert) => (
                    <div key={alert.id} className="udhr-card">
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                        <span className={`udhr-tag ${alert.severity === 'CRITICAL' ? 'danger' : 'info'}`}>{alert.severity} WARNING</span>
                        <span className="udhr-row-subtitle">{new Date(alert.createdAt).toLocaleDateString()}</span>
                      </div>
                      <p style={{ margin: 0, fontSize: '13.5px', color: 'var(--udhr-text-secondary)' }}>{alert.message}</p>
                    </div>
                  ))}
                </>
              )}

              {((healthGuidance?.dietaryGuidelines?.length > 0) || (healthGuidance?.healthTips?.length > 0)) && (
                <>
                  <h2 className="udhr-section-title" style={{ marginTop: '24px' }}>Dietary guidelines & lifestyle tips</h2>
                  {healthGuidance?.dietaryGuidelines?.filter(g => g.foodType === 'EAT').map((item) => (
                    <div key={`eat-${item.id}`} className="udhr-card">
                      <p className="udhr-row-title" style={{ color: 'var(--udhr-success)' }}>✓ Eat: {item.foodItem}</p>
                      <p className="udhr-row-subtitle">{item.description}</p>
                    </div>
                  ))}
                  {healthGuidance?.dietaryGuidelines?.filter(g => g.foodType === 'AVOID').map((item) => (
                    <div key={`avoid-${item.id}`} className="udhr-card">
                      <p className="udhr-row-title" style={{ color: 'var(--udhr-danger)' }}>✕ Avoid: {item.foodItem}</p>
                      <p className="udhr-row-subtitle">{item.description}</p>
                    </div>
                  ))}
                  {healthGuidance?.healthTips?.map((tip) => (
                    <div key={`tip-${tip.id}`} className="udhr-card">
                      <p className="udhr-row-title">{tip.title} <span className="udhr-tag info" style={{ marginLeft: '6px' }}>{tip.tipType}</span></p>
                      <p className="udhr-row-subtitle">{tip.description}</p>
                    </div>
                  ))}
                </>
              )}

              {healthGuidance?.medicationWarnings && Object.keys(healthGuidance.medicationWarnings).length > 0 && (
                <>
                  <h2 className="udhr-section-title" style={{ marginTop: '24px' }}>Medication allergy warnings</h2>
                  {Object.entries(healthGuidance.medicationWarnings).map(([allergen, warnings]) => (
                    <div key={allergen} className="udhr-card">
                      <p className="udhr-row-title">Allergen: {allergen.toUpperCase()}</p>
                      {warnings.length === 0 ? (
                        <p className="udhr-empty-note">No FDA alerts found for this allergen. Consult your doctor.</p>
                      ) : (
                        warnings.map((w, idx) => (
                          <p key={idx} className="udhr-row-subtitle" style={{ marginTop: '8px' }}>
                            ⚠️ Avoid <strong style={{ color: 'var(--udhr-text)' }}>{w.genericName}</strong> ({w.brandName}) — {w.warningText}
                          </p>
                        ))
                      )}
                    </div>
                  ))}
                </>
              )}
            </>
          )}

          {/* ===== Symptom Checker ===== */}
          {patientPortalTab === 'symptoms' && (
            <>
              <p style={{ color: 'var(--udhr-text-muted)', fontSize: '13.5px', margin: '0 0 14px' }}>
                Select what you're feeling and our care navigation engine (powered by Infermedica) will guide you on next steps. <em>This is not a diagnosis.</em>
              </p>
              <div className="udhr-chip-row">
                {symptomsList.map((symptom) => {
                  const isSelected = selectedSymptoms.includes(symptom.id);
                  return (
                    <button
                      key={symptom.id}
                      type="button"
                      className={`udhr-chip ${isSelected ? 'active' : ''}`}
                      onClick={() => handleSymptomToggle(symptom.id)}
                    >
                      {symptom.name}
                    </button>
                  );
                })}
              </div>
              <div style={{ display: 'flex', gap: '12px', marginBottom: '20px' }}>
                <button
                  type="button"
                  className="udhr-btn-primary"
                  style={{ width: 'auto', padding: '12px 20px' }}
                  onClick={handleSymptomCheckSubmit}
                  disabled={selectedSymptoms.length === 0 || loading}
                >
                  {loading ? 'Analyzing...' : 'Get guidance'}
                </button>
                {selectedSymptoms.length > 0 && (
                  <button type="button" className="udhr-btn-neutral" onClick={() => setSelectedSymptoms([])}>
                    Clear selection
                  </button>
                )}
              </div>

              {triageResult && (
                <div className={`udhr-result-card ${triageResult.urgencyLevel === 'RED' ? 'danger' : triageResult.urgencyLevel === 'YELLOW' ? 'warning' : 'success'}`}>
                  <span className={`udhr-tag ${triageResult.urgencyLevel === 'RED' ? 'danger' : 'info'}`}>
                    {triageResult.urgencyLevel === 'RED' ? '🔴 High · go to Emergency' : triageResult.urgencyLevel === 'YELLOW' ? '🟡 Moderate · visit a clinic within 24h' : '🟢 Low · rest & monitor at home'}
                  </span>
                  <p style={{ margin: '10px 0 0', fontSize: '13.5px', color: 'var(--udhr-text-secondary)' }}>
                    {triageResult.recommendation.split('[')[0]}
                  </p>
                  <p style={{ margin: '10px 0 0', fontSize: '12px', color: 'var(--udhr-text-muted)' }}>
                    ⚠️ This tool only provides care recommendations based on symptoms. It does not replace professional medical evaluation.
                  </p>
                  <button type="button" className="udhr-btn-neutral" style={{ marginTop: '12px' }} onClick={() => setTriageResult(null)}>
                    Acknowledge & close
                  </button>
                </div>
              )}

              {triageHistory.length > 0 && (
                <>
                  <h2 className="udhr-section-title" style={{ marginTop: '24px' }}>Symptom check history</h2>
                  {triageHistory.map((check) => (
                    <div key={check.id} className="udhr-card">
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                        <span className="udhr-row-subtitle">{new Date(check.checkedAt).toLocaleDateString()}</span>
                        <span className={`udhr-tag ${check.urgencyLevel === 'RED' ? 'danger' : 'info'}`}>{check.urgencyLevel}</span>
                      </div>
                      <p style={{ margin: 0, fontSize: '13.5px' }}>{check.recommendation.split('[')[0]}</p>
                      {check.details && check.details.length > 0 && (
                        <div className="udhr-chip-row" style={{ marginTop: '8px', marginBottom: 0 }}>
                          {check.details.map((d, idx) => (
                            <span key={idx} className="udhr-tag info">🩺 {d.symptom.name}</span>
                          ))}
                        </div>
                      )}
                    </div>
                  ))}
                </>
              )}
            </>
          )}

          {/* ===== Medications ===== */}
          {patientPortalTab === 'medications' && (
            <>
              {reminderData?.stats && reminderData.stats.totalDoses > 0 && (
                <div className="udhr-card" style={{ marginBottom: '20px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px', alignItems: 'center' }}>
                    <span style={{ fontSize: '14px', fontWeight: 600 }}>Weekly adherence score</span>
                    <span style={{ fontSize: '17px', fontWeight: 700, color: reminderData.stats.adherenceScore >= 80 ? 'var(--udhr-success)' : reminderData.stats.adherenceScore >= 50 ? 'var(--udhr-warning)' : 'var(--udhr-danger)' }}>
                      {reminderData.stats.adherenceScore}%
                    </span>
                  </div>
                  <div className="udhr-progress-track">
                    <div className="udhr-progress-fill" style={{ width: `${reminderData.stats.adherenceScore}%` }}></div>
                  </div>
                  <p className="udhr-row-subtitle">
                    You've taken {reminderData.stats.takenDoses} out of {reminderData.stats.totalDoses} scheduled doses this week. Keep it up!
                  </p>
                </div>
              )}

              {!reminderData || reminderData.adherenceLogs.length === 0 ? (
                <p className="udhr-empty-note">No medication reminders scheduled for today.</p>
              ) : (
                reminderData.adherenceLogs.map((log) => {
                  const timeString = new Date(log.scheduledTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
                  return (
                    <div key={log.id} className="udhr-row-card" style={{ alignItems: 'flex-start' }}>
                      <div style={{ flex: 1, minWidth: '200px' }}>
                        <p className="udhr-row-title">{log.reminder.prescription.medication} <span className="udhr-row-subtitle">({log.reminder.prescription.dosage})</span></p>
                        <p className="udhr-row-subtitle">{timeString} · {log.reminder.frequency}</p>
                        {log.takenAt && (
                          <p className="udhr-row-subtitle" style={{ color: 'var(--udhr-success)' }}>
                            Taken at {new Date(log.takenAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                          </p>
                        )}
                        {log.status === 'PENDING' ? (
                          <input
                            type="text"
                            className="udhr-note-input"
                            placeholder="How does this make you feel? (e.g. side effects, dizzy...)"
                            value={adherenceNotes[log.id] || ''}
                            onChange={(e) => setAdherenceNotes({ ...adherenceNotes, [log.id]: e.target.value })}
                          />
                        ) : (
                          log.notes && (
                            <p className="udhr-row-subtitle" style={{ fontStyle: 'italic' }}>Patient feedback: "{log.notes}"</p>
                          )
                        )}
                      </div>
                      <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                        {log.status === 'PENDING' ? (
                          <>
                            <button type="button" className="udhr-btn-soft-success" onClick={() => handleUpdateAdherence(log.id, 'TAKEN', adherenceNotes[log.id] || '')}>Taken</button>
                            <button type="button" className="udhr-btn-neutral" onClick={() => handleUpdateAdherence(log.id, 'MISSED', adherenceNotes[log.id] || '')}>Skip</button>
                          </>
                        ) : (
                          <span className={`udhr-tag ${log.status === 'TAKEN' ? 'info' : 'danger'}`}>{log.status}</span>
                        )}
                      </div>
                    </div>
                  );
                })
              )}
            </>
          )}

          {/* ===== Food Checker ===== */}
          {patientPortalTab === 'food' && (
            <>
              <p style={{ color: 'var(--udhr-text-muted)', fontSize: '13.5px', margin: '0 0 16px' }}>
                Type an ingredients list, look one up via Open Food Facts, or scan a label photo (OCR) to check it against your medical record.
              </p>

              <div className="udhr-segmented" style={{ maxWidth: '460px' }}>
                <button type="button" className={`udhr-segmented-btn ${foodInputMethod === 'type' ? 'active' : ''}`} onClick={() => setFoodInputMethod('type')}>Type</button>
                <button type="button" className={`udhr-segmented-btn ${foodInputMethod === 'search' ? 'active' : ''}`} onClick={() => setFoodInputMethod('search')}>Open Food Facts</button>
                <button type="button" className={`udhr-segmented-btn ${foodInputMethod === 'upload' ? 'active' : ''}`} onClick={() => setFoodInputMethod('upload')}>Upload Label</button>
              </div>

              {foodInputMethod === 'type' && (
                <div className="udhr-form-group" style={{ marginTop: '16px' }}>
                  <label className="udhr-label">Ingredients (separate with commas)</label>
                  <textarea
                    className="udhr-textarea"
                    value={ingredientsInput}
                    onChange={(e) => setIngredientsInput(e.target.value)}
                    placeholder="e.g. Sugar, Wheat Flour, Sodium Chloride, Peanut Butter, Vegetable Fat, Milk..."
                    rows={4}
                  />
                </div>
              )}

              {foodInputMethod === 'search' && (
                <div style={{ marginTop: '16px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
                  <div style={{ display: 'flex', gap: '12px' }}>
                    <div style={{ width: '150px' }} className="udhr-form-group">
                      <label className="udhr-label">Lookup type</label>
                      <select className="udhr-input" value={lookupType} onChange={(e) => setLookupType(e.target.value)}>
                        <option value="barcode">Barcode</option>
                        <option value="search">Product Name</option>
                      </select>
                    </div>
                    <div style={{ flex: 1 }} className="udhr-form-group">
                      <label className="udhr-label">{lookupType === 'barcode' ? 'Product Barcode' : 'Search Terms'}</label>
                      <div style={{ display: 'flex', gap: '8px' }}>
                        <input
                          type="text"
                          className="udhr-input"
                          value={productQuery}
                          onChange={(e) => setProductQuery(e.target.value)}
                          placeholder={lookupType === 'barcode' ? 'e.g. 737628064502' : 'e.g. wheat bread'}
                        />
                        <button type="button" className="udhr-btn-neutral" style={{ whiteSpace: 'nowrap' }} onClick={handleProductLookup} disabled={loading}>
                          Fetch
                        </button>
                      </div>
                    </div>
                  </div>
                  {ingredientsInput && (
                    <div className="udhr-form-group">
                      <label className="udhr-label">Fetched ingredients</label>
                      <textarea className="udhr-textarea" value={ingredientsInput} onChange={(e) => setIngredientsInput(e.target.value)} rows={2} />
                    </div>
                  )}
                </div>
              )}

              {foodInputMethod === 'upload' && (
                <div style={{ marginTop: '16px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
                  <label className="udhr-label">Upload food label photo</label>
                  <div style={{ border: '2px dashed var(--udhr-border)', borderRadius: '12px', padding: '24px', textAlign: 'center', background: '#f8fafc', position: 'relative', cursor: 'pointer' }}>
                    <Upload size={28} color="#94a3b8" style={{ margin: '0 auto 8px' }} />
                    <p style={{ fontSize: '13.5px', color: 'var(--udhr-text-muted)', margin: 0 }}>Choose label file or drag it here</p>
                    <p style={{ fontSize: '12px', color: 'var(--udhr-text-muted-2)', marginTop: '4px' }}>PNG, JPG or JPEG. Max size 5MB.</p>
                    <input type="file" accept="image/*" onChange={handleOcrUpload} style={{ position: 'absolute', top: 0, left: 0, width: '100%', height: '100%', opacity: 0, cursor: 'pointer' }} />
                  </div>
                  {ingredientsInput && (
                    <div className="udhr-form-group">
                      <label className="udhr-label">Extracted ingredients (OCR text)</label>
                      <textarea className="udhr-textarea" value={ingredientsInput} onChange={(e) => setIngredientsInput(e.target.value)} rows={2} />
                    </div>
                  )}
                  <p style={{ fontSize: '12.5px', color: 'var(--udhr-text-muted)' }}>
                    💡 Demo trigger: pick any file whose name contains <code>juice</code>, <code>chips</code>, or <code>bread</code> to auto-extract matching ingredients.
                  </p>
                </div>
              )}

              {ingredientsInput && (
                <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '16px' }}>
                  <button type="button" className="udhr-btn-primary" style={{ width: 'auto', padding: '11px 20px' }} onClick={handleCheckIngredients} disabled={loading}>
                    {loading ? 'Analyzing...' : 'Check against my record'}
                  </button>
                </div>
              )}

              {checkResults.length > 0 && (
                <div style={{ marginTop: '24px' }}>
                  <h2 className="udhr-section-title">Scanned ingredients analysis</h2>
                  {checkResults.map((res, i) => (
                    <div key={i} className="udhr-row-card">
                      <div>
                        <span className="udhr-row-title">{res.name}</span>
                        <p className="udhr-row-subtitle">{res.reason}</p>
                      </div>
                      <span className={`udhr-tag ${res.status === 'DANGER' ? 'danger' : 'info'}`}>{res.status}</span>
                    </div>
                  ))}
                </div>
              )}
            </>
          )}

          {/* ===== Lab Results ===== */}
          {patientPortalTab === 'labs' && (
            <>
              <p style={{ color: 'var(--udhr-text-muted)', fontSize: '13.5px', margin: '0 0 14px' }}>
                Results your care team has recorded, most recent first.
              </p>
              {myLabResults.length === 0 ? (
                <p className="udhr-empty-note">No lab results recorded yet.</p>
              ) : (
                myLabResults.map((l) => (
                  <div key={l.id} className="udhr-row-card">
                    <p className="udhr-row-title">{l.testName}</p>
                    <p style={{ margin: 0, fontSize: '13.5px', fontWeight: 700 }}>
                      {l.result}{l.unit ? ` ${l.unit}` : ''} <span className="udhr-row-subtitle" style={{ fontWeight: 400 }}>{l.normalRange ? `(normal: ${l.normalRange})` : ''}</span>
                    </p>
                  </div>
                ))
              )}
            </>
          )}

          {/* ===== Referral Letter ===== */}
          {patientPortalTab === 'referral' && (
            <>
              {patientReferral ? (
                <div className="udhr-card" style={{ maxWidth: '480px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '14px' }}>
                    <div>
                      <p style={{ margin: '0 0 2px', fontSize: '12px', fontWeight: 700, color: 'var(--udhr-text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>Referral Letter</p>
                      <p style={{ margin: 0, fontSize: '14px', fontWeight: 700 }}>Present this at the front desk</p>
                    </div>
                    <span className={`udhr-tag ${patientReferral.urgency === 'EMERGENCY' ? 'danger' : patientReferral.urgency === 'URGENT' ? 'warning' : 'info'}`}>
                      {patientReferral.urgency.charAt(0) + patientReferral.urgency.slice(1).toLowerCase()}
                    </span>
                  </div>
                  <div style={{ border: '1px dashed var(--udhr-border)', borderRadius: '10px', padding: '16px', textAlign: 'center', marginBottom: '16px' }}>
                    <p style={{ margin: '0 0 4px', fontSize: '11px', fontWeight: 700, color: 'var(--udhr-text-muted)', textTransform: 'uppercase', letterSpacing: '0.06em' }}>Referral Code</p>
                    <p style={{ margin: 0, fontSize: '24px', fontWeight: 800, letterSpacing: '0.02em' }}>REF-{patientReferral.id}</p>
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', fontSize: '13.5px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', gap: '12px' }}><span className="udhr-row-subtitle">Referred to</span><strong style={{ textAlign: 'right' }}>{patientReferral.toFacility?.name}</strong></div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', gap: '12px' }}><span className="udhr-row-subtitle">Reason</span><strong style={{ textAlign: 'right' }}>{patientReferral.reason}</strong></div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', gap: '12px' }}><span className="udhr-row-subtitle">Issued by</span><strong style={{ textAlign: 'right' }}>Dr. {patientReferral.referredBy?.firstName} {patientReferral.referredBy?.lastName}</strong></div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', gap: '12px' }}><span className="udhr-row-subtitle">Issued</span><strong style={{ textAlign: 'right' }}>{new Date(patientReferral.referredAt).toLocaleDateString()}</strong></div>
                  </div>
                  <p className="udhr-empty-note" style={{ marginTop: '14px', marginBottom: 0, textAlign: 'left' }}>
                    Valid immediately — no approval wait. Show this code at {patientReferral.toFacility?.name}'s front desk.
                  </p>
                </div>
              ) : (
                <p className="udhr-empty-note">No active referral letter. Your doctor will issue one here if you're referred to another facility.</p>
              )}
            </>
          )}
        </div>

        {chatWidgetOpen && (
          <div className="udhr-chat-widget">
            <div className="udhr-chat-widget-header">
              <span><MessageCircle size={16} style={{ verticalAlign: '-3px', marginRight: '6px' }} />Health Assistant</span>
              <button type="button" className="udhr-chat-widget-close" onClick={() => setChatWidgetOpen(false)} aria-label="Close health assistant">
                <XCircle size={18} />
              </button>
            </div>
            <p style={{ color: 'var(--udhr-text-muted)', fontSize: '12px', margin: '10px 16px 0' }}>
              Not a diagnosis — for emergencies, call emergency services.
            </p>
            <div className="udhr-chat-messages">
              {chatMessages.map((msg, idx) => (
                <div key={idx} className={`udhr-chat-bubble ${msg.sender === 'user' ? 'user' : 'bot'} ${msg.urgencyLevel === 'RED' ? 'urgent' : ''}`}>
                  {msg.text.split('\n').map((line, i) => (
                    <p key={i} style={{ margin: i === 0 ? 0 : '6px 0 0' }}>{line}</p>
                  ))}
                </div>
              ))}
              {chatSending && (
                <div className="udhr-chat-bubble bot">
                  <p style={{ margin: 0 }}>Thinking…</p>
                </div>
              )}
            </div>
            <form className="udhr-chat-input-row" onSubmit={handleSendChatMessage}>
              <input
                type="text"
                className="udhr-note-input"
                style={{ flex: 1 }}
                placeholder="e.g. I have a headache and fever"
                value={chatInput}
                onChange={(e) => setChatInput(e.target.value)}
                disabled={chatSending}
              />
              <button type="submit" className="udhr-btn-primary" style={{ width: 'auto', padding: '10px 16px' }} disabled={chatSending || !chatInput.trim()}>
                <Send size={16} />
              </button>
            </form>
          </div>
        )}

        {chatHintVisible && !chatWidgetOpen && (
          <div className="udhr-chat-hint">
            <button type="button" className="udhr-chat-hint-close" onClick={() => setChatHintVisible(false)} aria-label="Dismiss">
              <XCircle size={14} />
            </button>
            <p style={{ margin: 0 }}>
              👋 If you want to know about your medications, diet, or a symptom, you can ask me!
            </p>
          </div>
        )}

        <button
          type="button"
          className="udhr-chat-fab"
          onClick={() => { setChatWidgetOpen((open) => !open); setChatHintVisible(false); }}
          aria-label={chatWidgetOpen ? 'Close health assistant' : 'Open health assistant'}
        >
          {chatWidgetOpen ? <XCircle size={26} /> : <MessageCircle size={26} />}
        </button>
      </div>
    );
  }

  // 3. Doctor + Nurse "Clinical" dashboard — light clinical redesign. Sidebar
  // navigation (Patients/Vitals/Alerts/Queue/Referrals) replaces the old
  // segmented tab bar + single long scroll. Reuses every existing state
  // value and handler as-is (activeTabStaff now drives the sidebar instead
  // of a tab strip); Pharmacist and Admin are unaffected and still render
  // via the shared return below, in their own upcoming redesign passes.
  if (token && (isDoctor || isNurse) && !mustChangePassword) {
    const clinicalNavItems = isNurse
      ? [
          { key: 'vitals', label: 'Vitals', icon: <Activity size={17} /> },
          { key: 'labs', label: 'Lab Results', icon: <FileSpreadsheet size={17} /> },
          { key: 'patients', label: 'Register', icon: <Users size={17} /> },
          { key: 'alerts', label: 'Alerts', icon: <ShieldAlert size={17} /> },
          { key: 'queue', label: 'Queue', icon: <Clock size={17} /> },
          { key: 'referrals', label: 'Referrals', icon: <CornerUpRight size={17} /> },
        ]
      : [
          { key: 'patients', label: 'Patients', icon: <Users size={17} /> },
          { key: 'alerts', label: 'Alerts', icon: <ShieldAlert size={17} /> },
          { key: 'queue', label: 'Queue', icon: <Clock size={17} /> },
          { key: 'referrals', label: 'Referrals', icon: <CornerUpRight size={17} /> },
        ];

    const selectNavTab = (key) => {
      setActiveTabStaff(key);
      setIsMobileNavOpen(false);
      if (key === 'alerts') fetchClinicalAlerts();
      if (key === 'queue') fetchFacilityQueueToday();
      if (key === 'referrals') fetchReferrals();
      if (key === 'vitals' && isNurse) fetchNurseVitalsQueue();
      if (key === 'patients' && isDoctor) fetchDoctorConsultQueue();
    };

    const queueStageLabel = {
      WAITING_VITALS: 'Waiting for vitals',
      VITALS_DONE: 'Waiting for doctor',
      DIAGNOSED: 'Waiting for pharmacy',
    };

    const renderQueuePanel = () => (
      <>
        <h1 className="udhr-page-title">Today's Queue</h1>
        <p className="udhr-page-subtitle">Everyone still in progress at your facility today, across the whole pipeline.</p>
        <div className="udhr-record-card">
          <div className="udhr-record-body">
            {facilityQueueToday.length === 0 ? (
              <p className="udhr-empty-note">No one in the queue right now.</p>
            ) : (
              facilityQueueToday.map(visit => (
                <div key={visit.id} className="udhr-list-row" style={{ flexWrap: 'wrap' }}>
                  <div>
                    <p className="udhr-row-title">{visit.patient.firstName} {visit.patient.lastName}</p>
                    <p className="udhr-row-subtitle">
                      {visit.patient.idNumber ? `ID: ${visit.patient.idNumber} · ` : ''}MRN: {visit.patient.mrn}
                    </p>
                    <p className="udhr-row-subtitle">{visit.reason} · waiting since {new Date(visit.visitDate).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</p>
                  </div>
                  <span className="udhr-tag info">{queueStageLabel[visit.status] || visit.status.replaceAll('_', ' ')}</span>
                </div>
              ))
            )}
          </div>
        </div>
      </>
    );

    const renderReferralsPanel = () => (
      <>
        <h1 className="udhr-page-title">Referrals</h1>
        <p className="udhr-page-subtitle">Incoming and outgoing referrals between facilities.</p>

        <div className="udhr-record-card" style={{ maxWidth: 'clamp(280px, 60%, 480px)', marginBottom: '20px' }}>
          <div className="udhr-record-body">
            <h3 style={{ fontSize: '14px', fontWeight: 700, margin: '0 0 6px' }}>Refer to another facility</h3>
            {searchedPatientRecord ? (
              <>
                <p className="udhr-empty-note" style={{ marginBottom: '14px' }}>
                  For {searchedPatientRecord.patient.firstName} {searchedPatientRecord.patient.lastName}. Load a different patient under "Patients" to refer someone else.
                </p>
                <form onSubmit={handleCreateReferral}>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    <select className="udhr-input" value={referralForm.toFacilityId} onChange={(e) => setReferralForm({ ...referralForm, toFacilityId: e.target.value })} required>
                      <option value="">Select destination facility...</option>
                      {facilitiesForReferral.map(f => (
                        <option key={f.id} value={f.id}>{f.name}</option>
                      ))}
                    </select>
                    <select className="udhr-input" value={referralForm.urgency} onChange={(e) => setReferralForm({ ...referralForm, urgency: e.target.value })}>
                      <option value="ROUTINE">Routine</option>
                      <option value="URGENT">Urgent</option>
                      <option value="EMERGENCY">Emergency</option>
                    </select>
                    <input type="text" className="udhr-input" placeholder="Reason for referral" value={referralForm.reason} onChange={(e) => setReferralForm({ ...referralForm, reason: e.target.value })} required />
                    <textarea className="udhr-textarea" placeholder="Clinical summary" value={referralForm.clinicalSummary} onChange={(e) => setReferralForm({ ...referralForm, clinicalSummary: e.target.value })} rows={2} />
                    <button type="submit" className="udhr-btn-primary" disabled={loading}>Send referral</button>
                  </div>
                </form>
              </>
            ) : (
              <p className="udhr-empty-note">Load a patient under "Patients" first, then come back here to refer them.</p>
            )}
          </div>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px,1fr))', gap: '16px' }}>
          <div>
            <p style={{ fontSize: '12px', fontWeight: 700, color: 'var(--udhr-text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em', margin: '0 0 10px' }}>Incoming</p>
            {referralInbox.length === 0 ? (
              <p className="udhr-empty-note">No incoming referrals for your facility.</p>
            ) : (
              referralInbox.map(r => (
                <div key={r.id} className="udhr-card" style={{ marginBottom: '8px', padding: '12px 14px' }}>
                  <p className="udhr-row-title">{r.patient.firstName} {r.patient.lastName}</p>
                  <p className="udhr-row-subtitle">from {r.fromFacility.name} · <span className={`udhr-tag ${r.urgency === 'EMERGENCY' ? 'danger' : 'info'}`} style={{ marginLeft: '2px' }}>{r.urgency}</span></p>
                  <p className="udhr-row-subtitle">{r.reason}</p>
                  {r.status === 'PENDING' ? (
                    <div style={{ display: 'flex', gap: '8px', marginTop: '8px' }}>
                      <button type="button" className="udhr-btn-soft-success" onClick={() => handleRespondReferral(r.id, 'ACCEPTED')}>Accept</button>
                      <button type="button" className="udhr-btn-neutral" onClick={() => handleRespondReferral(r.id, 'DECLINED')}>Decline</button>
                    </div>
                  ) : (
                    <p className="udhr-row-subtitle" style={{ marginTop: '6px', fontWeight: 600 }}>{r.status}</p>
                  )}
                </div>
              ))
            )}
          </div>
          <div>
            <p style={{ fontSize: '12px', fontWeight: 700, color: 'var(--udhr-text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em', margin: '0 0 10px' }}>Outgoing</p>
            {referralOutgoing.length === 0 ? (
              <p className="udhr-empty-note">
                No outgoing referrals yet. Use "Refer to another facility" above.
              </p>
            ) : (
              referralOutgoing.map(r => (
                <div key={r.id} className="udhr-card" style={{ marginBottom: '8px', padding: '12px 14px' }}>
                  <p className="udhr-row-title">{r.patient.firstName} {r.patient.lastName} → {r.toFacility.name}</p>
                  <p className="udhr-row-subtitle">{r.reason}</p>
                  <p className="udhr-row-subtitle" style={{ fontWeight: 600 }}>{r.status}</p>
                </div>
              ))
            )}
          </div>
        </div>
      </>
    );

    const renderRegistrationFields = () => (
      <>
        <div className="udhr-form-group">
          <label className="udhr-label">ID Number</label>
          <input type="text" className="udhr-input" value={patientRegForm.idNumber} onChange={(e) => setPatientRegForm({ ...patientRegForm, idNumber: e.target.value.replace(/\D/g, '').slice(0, 13) })} placeholder="SA ID number (optional)" maxLength={13} inputMode="numeric" pattern="[0-9]*" />
        </div>
        <div className="udhr-form-group">
          <label className="udhr-label">Passport Number</label>
          <input type="text" className="udhr-input" value={patientRegForm.passportNumber} onChange={(e) => setPatientRegForm({ ...patientRegForm, passportNumber: e.target.value })} placeholder="Passport number (optional)" />
        </div>
        <p className="udhr-empty-note" style={{ marginBottom: '12px' }}>
          An MRN is auto-assigned to every patient regardless — ID number and passport number are optional.
        </p>
        <div className="udhr-form-group">
          <label className="udhr-label">First Name</label>
          <input type="text" className="udhr-input" value={patientRegForm.firstName} onChange={(e) => setPatientRegForm({ ...patientRegForm, firstName: e.target.value })} required />
        </div>
        <div className="udhr-form-group">
          <label className="udhr-label">Last Name</label>
          <input type="text" className="udhr-input" value={patientRegForm.lastName} onChange={(e) => setPatientRegForm({ ...patientRegForm, lastName: e.target.value })} required />
        </div>
        <div className="udhr-form-group">
          <label className="udhr-label">Date of Birth</label>
          <input type="date" className="udhr-input" value={patientRegForm.dateOfBirth} onChange={(e) => setPatientRegForm({ ...patientRegForm, dateOfBirth: e.target.value })} required max={new Date().toISOString().split('T')[0]} />
        </div>
        <div className="udhr-form-group">
          <label className="udhr-label">Gender</label>
          <select className="udhr-input" value={patientRegForm.gender} onChange={(e) => setPatientRegForm({ ...patientRegForm, gender: e.target.value })} required>
            <option value="" disabled>Select gender</option>
            <option value="MALE">Male</option>
            <option value="FEMALE">Female</option>
            <option value="OTHER">Other</option>
          </select>
        </div>
        <div className="udhr-form-group">
          <label className="udhr-label">Contact Number</label>
          <input type="text" className="udhr-input" value={patientRegForm.contactNumber} onChange={(e) => setPatientRegForm({ ...patientRegForm, contactNumber: e.target.value.replace(/\D/g, '') })} inputMode="numeric" pattern="[0-9]*" />
        </div>
        <div className="udhr-form-group">
          <label className="udhr-label">Email Address</label>
          <input type="email" className="udhr-input" value={patientRegForm.email} onChange={(e) => setPatientRegForm({ ...patientRegForm, email: e.target.value })} placeholder="patient@gmail.com" />
        </div>
        <div className="udhr-form-group">
          <label className="udhr-label">Address</label>
          <textarea className="udhr-textarea" value={patientRegForm.address} onChange={(e) => setPatientRegForm({ ...patientRegForm, address: e.target.value })} rows={2} />
        </div>
        <button
          type="button"
          className="udhr-btn-neutral"
          style={{ width: '100%', marginBottom: '10px' }}
          onClick={() => setShowRegExtras(!showRegExtras)}
        >
          {showRegExtras ? 'Hide' : 'Add'} Next of Kin / Newborn Details (optional)
        </button>
        {showRegExtras && (
          <div style={{ background: '#f8fafc', border: '1px solid var(--udhr-border)', padding: '12px', borderRadius: '8px', marginBottom: '10px' }}>
            <p className="udhr-empty-note" style={{ marginBottom: '8px' }}>Next of Kin</p>
            <div className="udhr-form-group"><input type="text" className="udhr-input" placeholder="First name" value={patientRegForm.nextOfKinFirstName} onChange={(e) => setPatientRegForm({ ...patientRegForm, nextOfKinFirstName: e.target.value })} /></div>
            <div className="udhr-form-group"><input type="text" className="udhr-input" placeholder="Last name" value={patientRegForm.nextOfKinLastName} onChange={(e) => setPatientRegForm({ ...patientRegForm, nextOfKinLastName: e.target.value })} /></div>
            <div className="udhr-form-group"><input type="text" className="udhr-input" placeholder="Relationship (e.g. Husband)" value={patientRegForm.nextOfKinRelationship} onChange={(e) => setPatientRegForm({ ...patientRegForm, nextOfKinRelationship: e.target.value })} /></div>
            <div className="udhr-form-group"><input type="text" className="udhr-input" placeholder="Phone number" value={patientRegForm.nextOfKinPhone} onChange={(e) => setPatientRegForm({ ...patientRegForm, nextOfKinPhone: e.target.value })} /></div>
            <p className="udhr-empty-note" style={{ margin: '12px 0 8px' }}>Newborn — leave blank unless registering a baby at birth</p>
            <div className="udhr-form-group"><input type="text" className="udhr-input" placeholder="Mother's ID number" value={patientRegForm.motherIdNumber} onChange={(e) => setPatientRegForm({ ...patientRegForm, motherIdNumber: e.target.value })} /></div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
              <input type="number" className="udhr-input" placeholder="Birth weight (g)" value={patientRegForm.birthWeightGrams} onChange={(e) => setPatientRegForm({ ...patientRegForm, birthWeightGrams: e.target.value })} />
              <input type="number" className="udhr-input" placeholder="Birth length (cm)" value={patientRegForm.birthLengthCm} onChange={(e) => setPatientRegForm({ ...patientRegForm, birthLengthCm: e.target.value })} />
              <input type="number" className="udhr-input" placeholder="Apgar (1 min)" value={patientRegForm.apgarScore1Min} onChange={(e) => setPatientRegForm({ ...patientRegForm, apgarScore1Min: e.target.value })} />
              <input type="number" className="udhr-input" placeholder="Apgar (5 min)" value={patientRegForm.apgarScore5Min} onChange={(e) => setPatientRegForm({ ...patientRegForm, apgarScore5Min: e.target.value })} />
            </div>
          </div>
        )}
      </>
    );

    const renderNurseRegisterPanel = () => (
      <>
        <h1 className="udhr-page-title">Register</h1>
        <p className="udhr-page-subtitle">Register a new patient at this facility.</p>
        <div className="udhr-record-card" style={{ maxWidth: 'clamp(320px, 60%, 720px)' }}>
          <div className="udhr-record-body">
            <form onSubmit={handleRegisterPatient}>
              {renderRegistrationFields()}
              <button type="submit" className="udhr-btn-primary">Create Patient Record</button>
            </form>
          </div>
        </div>
      </>
    );

    const renderNurseVitalsPanel = () => (
      <>
        <h1 className="udhr-page-title">Capture Vitals</h1>
        <p className="udhr-page-subtitle">Patients admin has checked in and queued for vitals — pick one from the list below.</p>

        {!vitalsPatient && (
          <>
            <p className="udhr-label" style={{ marginBottom: '8px' }}>Look up by ID/MRN (e.g. a walk-in who bypassed the front desk)</p>
            <form onSubmit={handleLookupForVitals} className="udhr-search-bar" style={{ marginBottom: '20px' }}>
              <input
                type="text"
                className="udhr-input"
                value={vitalsSearchId}
                onChange={(e) => setVitalsSearchId(e.target.value)}
                placeholder="Enter patient ID number or MRN"
                required
              />
              <button type="submit" className="udhr-btn-compact" disabled={loading}>{loading ? 'Searching...' : 'Find'}</button>
            </form>

            <p className="udhr-label" style={{ marginBottom: '8px' }}>Or pick from the queue</p>
            <div className="udhr-record-card" style={{ maxWidth: 'clamp(320px, 60%, 720px)' }}>
              <div className="udhr-record-body">
                {nurseVitalsQueue.length === 0 ? (
                  <p className="udhr-empty-note">No one waiting for vitals right now. Admin queues patients at check-in.</p>
                ) : (
                  nurseVitalsQueue.map(visit => (
                    <div key={visit.id} className="udhr-list-row">
                      <div>
                        <p className="udhr-row-title">{visit.patient.firstName} {visit.patient.lastName}</p>
                        <p className="udhr-row-subtitle">
                          {visit.patient.idNumber ? `ID: ${visit.patient.idNumber} · ` : ''}MRN: {visit.patient.mrn}
                        </p>
                        <p className="udhr-row-subtitle">{visit.reason} · waiting since {new Date(visit.visitDate).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</p>
                      </div>
                      <button type="button" className="udhr-btn-primary" onClick={() => handleSelectFromVitalsQueue(visit)}>Take vitals</button>
                    </div>
                  ))
                )}
              </div>
            </div>
          </>
        )}

        {vitalsPatient && (
          <>
            <button type="button" className="udhr-btn-neutral" onClick={handleBackToVitalsQueue} style={{ marginBottom: '16px' }}>← Back to queue</button>
            <div className="udhr-record-card" style={{ maxWidth: 'clamp(320px, 60%, 720px)', marginBottom: '16px' }}>
              <div className="udhr-record-body">
                <p className="udhr-row-title" style={{ fontSize: '14px', marginBottom: '2px' }}>{vitalsPatient.firstName} {vitalsPatient.lastName}</p>
                <p className="udhr-row-subtitle" style={{ marginBottom: '14px' }}>
                  {vitalsPatient.idNumber ? `ID: ${vitalsPatient.idNumber} · ` : ''}MRN: {vitalsPatient.mrn} · {vitalsPatient.gender} · DOB {vitalsPatient.dateOfBirth}
                </p>
                <form onSubmit={handleRecordVitals}>
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', marginBottom: '10px' }}>
                    <input type="text" className="udhr-input" placeholder="Blood pressure (e.g. 120/80)" value={vitalsForm.bloodPressure} onChange={(e) => setVitalsForm({ ...vitalsForm, bloodPressure: e.target.value })} />
                    <input type="number" step="0.1" className="udhr-input" placeholder="Temp (°C)" value={vitalsForm.temperatureCelsius} onChange={(e) => setVitalsForm({ ...vitalsForm, temperatureCelsius: e.target.value })} />
                  </div>
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', marginBottom: '10px' }}>
                    <input type="number" className="udhr-input" placeholder="Pulse (bpm)" value={vitalsForm.pulseBpm} onChange={(e) => setVitalsForm({ ...vitalsForm, pulseBpm: e.target.value })} />
                    <input type="number" className="udhr-input" placeholder="Respiration rate" value={vitalsForm.respirationRate} onChange={(e) => setVitalsForm({ ...vitalsForm, respirationRate: e.target.value })} />
                  </div>
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '10px', marginBottom: '10px' }}>
                    <input type="number" className="udhr-input" placeholder="SpO2 (%)" value={vitalsForm.oxygenSaturation} onChange={(e) => setVitalsForm({ ...vitalsForm, oxygenSaturation: e.target.value })} />
                    <input type="number" step="0.1" className="udhr-input" placeholder="Weight (kg)" value={vitalsForm.weightKg} onChange={(e) => setVitalsForm({ ...vitalsForm, weightKg: e.target.value })} />
                    <input type="number" step="0.1" className="udhr-input" placeholder="Height (cm)" value={vitalsForm.heightCm} onChange={(e) => setVitalsForm({ ...vitalsForm, heightCm: e.target.value })} />
                  </div>
                  <div className="udhr-form-group">
                    <textarea className="udhr-textarea" rows={2} placeholder="Anything the doctor should know before seeing the patient..." value={vitalsForm.notes} onChange={(e) => setVitalsForm({ ...vitalsForm, notes: e.target.value })} />
                  </div>
                  <button type="submit" className="udhr-btn-primary" disabled={loading}>{loading ? 'Saving...' : 'Save vitals & send to doctor'}</button>
                </form>
              </div>
            </div>
          </>
        )}
      </>
    );

    const renderNurseLabsPanel = () => (
      <>
        <h1 className="udhr-page-title">Lab Results</h1>
        <p className="udhr-page-subtitle">Look a patient up by ID or MRN to attach a lab result to their file.</p>

        {!labsPatient && (
          <form onSubmit={handleLookupForLabs} className="udhr-search-bar">
            <input
              type="text"
              className="udhr-input"
              value={labsSearchId}
              onChange={(e) => setLabsSearchId(e.target.value)}
              placeholder="Enter patient ID number or MRN"
              required
            />
            <button type="submit" className="udhr-btn-compact" disabled={loading}>{loading ? 'Searching...' : 'Find'}</button>
          </form>
        )}

        {labsPatient && (
          <>
            <button type="button" className="udhr-btn-neutral" onClick={handleBackToLabsSearch} style={{ marginBottom: '16px' }}>← Search another patient</button>
            <div className="udhr-record-card" style={{ maxWidth: 'clamp(320px, 60%, 720px)' }}>
              <div className="udhr-record-body">
                <p className="udhr-row-title" style={{ fontSize: '14px', marginBottom: '2px' }}>{labsPatient.firstName} {labsPatient.lastName}</p>
                <p className="udhr-row-subtitle" style={{ marginBottom: '14px' }}>
                  {labsPatient.idNumber ? `ID: ${labsPatient.idNumber} · ` : ''}MRN: {labsPatient.mrn} · {labsPatient.gender} · DOB {labsPatient.dateOfBirth}
                </p>
                <form onSubmit={handleAddLabResult}>
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', marginBottom: '10px' }}>
                    <input type="text" className="udhr-input" placeholder="Test name (e.g. HbA1c)" value={labResultForm.testName} onChange={(e) => setLabResultForm({ ...labResultForm, testName: e.target.value })} required />
                    <input type="text" className="udhr-input" placeholder="Result" value={labResultForm.result} onChange={(e) => setLabResultForm({ ...labResultForm, result: e.target.value })} required />
                  </div>
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', marginBottom: '10px' }}>
                    <input type="text" className="udhr-input" placeholder="Unit (e.g. g/dL)" value={labResultForm.unit} onChange={(e) => setLabResultForm({ ...labResultForm, unit: e.target.value })} />
                    <input type="text" className="udhr-input" placeholder="Normal range" value={labResultForm.normalRange} onChange={(e) => setLabResultForm({ ...labResultForm, normalRange: e.target.value })} />
                  </div>
                  <div className="udhr-form-group">
                    <textarea className="udhr-textarea" rows={2} placeholder="Notes" value={labResultForm.notes} onChange={(e) => setLabResultForm({ ...labResultForm, notes: e.target.value })} />
                  </div>
                  <button type="submit" className="udhr-btn-primary" style={{ width: '100%' }} disabled={loading}>Save result</button>
                </form>
              </div>
            </div>
          </>
        )}
      </>
    );

    const recordSubTabs = [
      ['add', 'Diagnose & Prescribe'],
      ['diagnoses', 'Diagnoses'],
      ['prescriptions', 'Prescriptions'],
      ['labs', 'Labs'],
      ['vitals', 'Vitals'],
      ['conditions', 'Conditions'],
      ['immunizations', 'Immunizations'],
      ['timeline', 'Timeline'],
    ];

    const renderDoctorPatientsPanel = () => (
      <>
        <h1 className="udhr-page-title">Patients</h1>
        <p className="udhr-page-subtitle">Patients the nurse has taken vitals for and sent through to you — pick one from the list below.</p>

        {!searchedPatientRecord && (
          <>
            <p className="udhr-label" style={{ marginBottom: '8px' }}>Look up by ID/MRN (e.g. a follow-up not on today's queue)</p>
            <form onSubmit={handleSearchPatient} className="udhr-search-bar" style={{ marginBottom: '20px' }}>
              <input
                type="text"
                className="udhr-input"
                value={searchId}
                onChange={(e) => setSearchId(e.target.value)}
                placeholder="Enter patient ID number or MRN"
                required
              />
              <button type="submit" className="udhr-btn-compact" disabled={loading}>{loading ? 'Searching...' : 'Search'}</button>
            </form>

            <p className="udhr-label" style={{ marginBottom: '8px' }}>Or pick from the queue</p>
            <div className="udhr-record-card">
              <div className="udhr-record-body">
                {doctorConsultQueue.length === 0 ? (
                  <p className="udhr-empty-note">No one waiting for consultation right now.</p>
                ) : (
                  doctorConsultQueue.map(visit => (
                    <div key={visit.id} className="udhr-list-row">
                      <div>
                        <p className="udhr-row-title">{visit.patient.firstName} {visit.patient.lastName}</p>
                        <p className="udhr-row-subtitle">
                          {visit.patient.idNumber ? `ID: ${visit.patient.idNumber} · ` : ''}MRN: {visit.patient.mrn}
                        </p>
                        <p className="udhr-row-subtitle">{visit.reason} · waiting since {new Date(visit.visitDate).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</p>
                      </div>
                      <button type="button" className="udhr-btn-primary" onClick={() => handleSelectFromDoctorQueue(visit)} disabled={loading}>See patient</button>
                    </div>
                  ))
                )}
              </div>
            </div>
          </>
        )}

        {searchedPatientRecord ? (
          <>
            <div ref={recordTopRef}>
              <button type="button" className="udhr-btn-neutral" onClick={handleBackToDoctorQueue} style={{ marginBottom: '16px' }}>← Back to queue</button>
            </div>
            <div className="udhr-record-card" style={{ marginBottom: '20px' }}>
              <div className="udhr-record-header">
                <div>
                  <p className="udhr-row-title" style={{ fontSize: '16px' }}>{searchedPatientRecord.patient.firstName} {searchedPatientRecord.patient.lastName}</p>
                  <p className="udhr-row-subtitle">
                    {searchedPatientRecord.patient.idNumber ? `ID: ${searchedPatientRecord.patient.idNumber} · ` : ''}MRN: {searchedPatientRecord.patient.mrn} · {searchedPatientRecord.patient.gender} · Born {searchedPatientRecord.patient.dateOfBirth}
                  </p>
                  <p className="udhr-row-subtitle">
                    Contact: {searchedPatientRecord.patient.contactNumber || 'N/A'} · Email: {searchedPatientRecord.patient.email || 'N/A'}
                  </p>
                  {(() => {
                    const lastVisit = searchedPatientRecord.visits && searchedPatientRecord.visits.length > 0
                      ? searchedPatientRecord.visits.reduce((latest, current) => new Date(current.visitDate) > new Date(latest.visitDate) ? current : latest)
                      : null;
                    return (
                      <p className="udhr-row-subtitle" style={{ marginTop: '6px' }}>
                        Last visit: {lastVisit ? `${new Date(lastVisit.visitDate).toLocaleDateString()} — ${lastVisit.reason}${lastVisit.notes ? ` (${lastVisit.notes})` : ''}` : 'No recorded visits'}
                      </p>
                    );
                  })()}
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', alignItems: 'flex-end' }}>
                  <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap', justifyContent: 'flex-end' }}>
                    {searchedPatientRecord.allergies.map(a => (
                      <span key={a.id} className="udhr-tag danger">{a.allergen} allergy</span>
                    ))}
                    {searchedPatientRecord.chronicConditions.map(c => (
                      <span key={c.id} className="udhr-tag info">{c.conditionName}</span>
                    ))}
                  </div>
                  {searchedPatientRecord.currentVisit ? (
                    <button
                      type="button"
                      onClick={() => {
                        if (activeRecordTab === 'vitals') {
                          setActiveRecordTab('add');
                          recordTopRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
                        } else {
                          setActiveRecordTab('vitals');
                          recordSubTabsRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
                        }
                      }}
                      title={activeRecordTab === 'vitals' ? 'Back to the top of this record' : "View vitals and the nurse's notes"}
                      style={{ fontSize: '11.5px', fontWeight: 700, color: 'var(--udhr-text-secondary)', background: 'var(--udhr-border-subtle)', padding: '4px 10px', borderRadius: '999px', border: 'none', cursor: 'pointer' }}
                    >
                      {activeRecordTab === 'vitals' ? '← Back' : `${searchedPatientRecord.currentVisit.status.replaceAll('_', ' ')} →`}
                    </button>
                  ) : (
                    <span style={{ fontSize: '11.5px', fontWeight: 700, color: 'var(--udhr-text-secondary)', background: 'var(--udhr-border-subtle)', padding: '4px 10px', borderRadius: '999px' }}>
                      No open visit
                    </span>
                  )}
                  <button type="button" className="udhr-btn-neutral" onClick={() => handleEvaluatePatient(searchedPatientRecord.patient.id)}>
                    <RefreshCw size={14} style={{ marginRight: '4px' }} /> Analyze response (CDS)
                  </button>
                </div>
              </div>

              <div className="udhr-record-subtabs" ref={recordSubTabsRef}>
                {recordSubTabs.map(([key, label]) => (
                  <button key={key} type="button" className={`udhr-subtab ${activeRecordTab === key ? 'active' : ''}`} onClick={() => setActiveRecordTab(key)}>
                    {label}
                  </button>
                ))}
              </div>

              <div className="udhr-record-body">
                {activeRecordTab === 'diagnoses' && (
                  searchedPatientRecord.diagnoses.length === 0 ? (
                    <p className="udhr-empty-note">No diagnoses recorded.</p>
                  ) : (
                    searchedPatientRecord.diagnoses.map(d => (
                      <div key={d.id} className="udhr-list-row" style={{ flexDirection: 'column', alignItems: 'flex-start' }}>
                        <p className="udhr-row-title">
                          {d.diagnosis} {d.icd10Code && <span className="udhr-tag info" style={{ marginLeft: '6px' }}>ICD-10: {d.icd10Code}</span>}
                          {d.facility?.name && <span className="udhr-tag neutral" style={{ marginLeft: '6px' }}>📍 {d.facility.name}</span>}
                        </p>
                        <p className="udhr-row-subtitle">Diagnosed: {new Date(d.diagnosedAt).toLocaleString()} · {d.notes}</p>
                      </div>
                    ))
                  )
                )}

                {activeRecordTab === 'prescriptions' && (
                  <>
                    {searchedPatientRecord.prescriptions.length === 0 ? (
                      <p className="udhr-empty-note">No prescriptions active.</p>
                    ) : (
                      searchedPatientRecord.prescriptions.map(p => (
                        <div key={p.id} className="udhr-list-row" style={{ flexDirection: 'column', alignItems: 'flex-start' }}>
                          <div style={{ display: 'flex', justifyContent: 'space-between', width: '100%', alignItems: 'center' }}>
                            <p className="udhr-row-title">
                              {p.medication}
                              {p.facility?.name && <span className="udhr-tag neutral" style={{ marginLeft: '6px' }}>📍 {p.facility.name}</span>}
                            </p>
                            <span className="udhr-pill-status">{p.dispensed ? (p.dispenseMethod === 'SELF' ? 'Dispensed by doctor' : 'Dispensed by pharmacy') : 'Waiting at pharmacy'}</span>
                          </div>
                          <p className="udhr-row-subtitle">{p.dosage} · {p.frequency} · {p.durationDays} days</p>
                          {p.notes && <p className="udhr-row-subtitle">{p.notes}</p>}
                        </div>
                      ))
                    )}
                    {patientAdherence && patientAdherence.stats.totalDoses > 0 && (
                      <div style={{ marginTop: '20px', paddingTop: '16px', borderTop: '1px dashed var(--udhr-border)' }}>
                        <h4 style={{ fontSize: '13px', fontWeight: 700, margin: '0 0 10px' }}>Adherence history</h4>
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '10px' }}>
                          <span style={{ fontSize: '13px' }}>Compliance score</span>
                          <span style={{ fontSize: '15px', fontWeight: 700, color: patientAdherence.stats.adherenceScore >= 80 ? 'var(--udhr-success)' : 'var(--udhr-warning)' }}>{patientAdherence.stats.adherenceScore}%</span>
                        </div>
                        {patientAdherence.adherenceLogs.map((log) => (
                          <div key={log.id} className="udhr-list-row">
                            <div>
                              <p className="udhr-row-title">{log.reminder.prescription.medication}</p>
                              <p className="udhr-row-subtitle">{new Date(log.scheduledTime).toLocaleString([], { dateStyle: 'short', timeStyle: 'short' })}</p>
                              {log.notes && <p className="udhr-row-subtitle" style={{ fontStyle: 'italic' }}>"{log.notes}"</p>}
                            </div>
                            <span className={`udhr-tag ${log.status === 'TAKEN' ? 'info' : 'danger'}`}>{log.status}</span>
                          </div>
                        ))}
                      </div>
                    )}
                  </>
                )}

                {activeRecordTab === 'labs' && (
                  searchedPatientRecord.labResults && searchedPatientRecord.labResults.length > 0 ? (
                    searchedPatientRecord.labResults.map(l => (
                      <div key={l.id} className="udhr-list-row">
                        <div>
                          <p className="udhr-row-title">
                            {l.testName}
                            {l.facility?.name && <span className="udhr-tag neutral" style={{ marginLeft: '6px' }}>📍 {l.facility.name}</span>}
                          </p>
                          {l.notes && <p className="udhr-row-subtitle" style={{ fontStyle: 'italic' }}>{l.notes}</p>}
                        </div>
                        <p style={{ margin: 0, fontSize: '13px', fontWeight: 700 }}>
                          {l.result} {l.unit || ''} <span className="udhr-row-subtitle" style={{ fontWeight: 400 }}>{l.normalRange ? `(normal: ${l.normalRange})` : ''}</span>
                        </p>
                      </div>
                    ))
                  ) : (
                    <p className="udhr-empty-note">No lab results on file for this patient.</p>
                  )
                )}

                {activeRecordTab === 'vitals' && (
                  searchedPatientRecord.vitals && searchedPatientRecord.vitals.length > 0 ? (
                    (() => {
                      const v = searchedPatientRecord.vitals[0];
                      return (
                        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(120px, 1fr))', gap: '14px' }}>
                          <div><p className="udhr-stat-label">Blood Pressure</p><p className="udhr-stat-value">{v.bloodPressure || 'N/A'}</p></div>
                          <div><p className="udhr-stat-label">Temperature</p><p className="udhr-stat-value">{v.temperatureCelsius != null ? `${v.temperatureCelsius}°C` : 'N/A'}</p></div>
                          <div><p className="udhr-stat-label">Pulse</p><p className="udhr-stat-value">{v.pulseBpm != null ? `${v.pulseBpm} bpm` : 'N/A'}</p></div>
                          <div><p className="udhr-stat-label">Respiration</p><p className="udhr-stat-value">{v.respirationRate != null ? `${v.respirationRate}/min` : 'N/A'}</p></div>
                          <div><p className="udhr-stat-label">O2 Saturation</p><p className="udhr-stat-value">{v.oxygenSaturation != null ? `${v.oxygenSaturation}%` : 'N/A'}</p></div>
                          <div><p className="udhr-stat-label">Weight / Height</p><p className="udhr-stat-value">{v.weightKg != null ? `${v.weightKg}kg` : 'N/A'} / {v.heightCm != null ? `${v.heightCm}cm` : 'N/A'}</p></div>
                          {v.notes && (
                            <div style={{ gridColumn: '1 / -1' }}>
                              <p className="udhr-stat-label">Nurse's notes</p>
                              <p style={{ margin: 0, fontSize: '13.5px' }}>{v.notes}</p>
                            </div>
                          )}
                          <div style={{ gridColumn: '1 / -1' }}>
                            <p className="udhr-row-subtitle">Recorded by {v.recordedBy ? `${v.recordedBy.firstName} ${v.recordedBy.lastName}` : 'nurse'} on {new Date(v.recordedAt).toLocaleString()}</p>
                          </div>
                        </div>
                      );
                    })()
                  ) : (
                    <p className="udhr-empty-note">No vitals recorded yet for this patient. Check with the nurse before consulting.</p>
                  )
                )}

                {activeRecordTab === 'conditions' && (
                  <>
                    <h4 style={{ fontSize: '13px', fontWeight: 700, margin: '0 0 8px' }}>Active chronic conditions</h4>
                    {searchedPatientRecord.chronicConditions.length === 0 ? (
                      <p className="udhr-empty-note" style={{ marginBottom: '16px' }}>No registered chronic conditions.</p>
                    ) : (
                      searchedPatientRecord.chronicConditions.map(c => (
                        <div key={c.id} className="udhr-list-row" style={{ flexDirection: 'column', alignItems: 'flex-start' }}>
                          <p className="udhr-row-title">{c.conditionName}</p>
                          <p className="udhr-row-subtitle">Diagnosed: {c.diagnosedDate} · {c.notes}</p>
                        </div>
                      ))
                    )}
                    <h4 style={{ fontSize: '13px', fontWeight: 700, margin: '16px 0 8px' }}>Allergies</h4>
                    {searchedPatientRecord.allergies.length === 0 ? (
                      <p className="udhr-empty-note">No registered drug or food allergies.</p>
                    ) : (
                      searchedPatientRecord.allergies.map(a => (
                        <div key={a.id} className="udhr-list-row" style={{ flexDirection: 'column', alignItems: 'flex-start' }}>
                          <p className="udhr-row-title">{a.allergen}</p>
                          <p className="udhr-row-subtitle">Severity: {a.severity} · {a.notes}</p>
                        </div>
                      ))
                    )}
                  </>
                )}

                {activeRecordTab === 'immunizations' && (
                  <>
                    {patientImmunizations.length === 0 && (
                      <button type="button" className="udhr-dashed-btn" style={{ marginBottom: '14px' }} onClick={() => handleGenerateImmunizationSchedule(searchedPatientRecord.patient.id)}>
                        Generate EPI schedule
                      </button>
                    )}
                    {patientImmunizations.length === 0 ? (
                      <p className="udhr-empty-note">No immunization records yet.</p>
                    ) : (
                      patientImmunizations.map(imm => (
                        <div key={imm.id} className="udhr-list-row">
                          <div>
                            <p className="udhr-row-title">{imm.vaccineName} {imm.doseNumber ? `(dose ${imm.doseNumber})` : ''}</p>
                            <p className="udhr-row-subtitle">Scheduled: {imm.scheduledDate} {imm.administeredDate ? `— Given: ${imm.administeredDate}` : ''}</p>
                          </div>
                          <div style={{ display: 'flex', gap: '6px', alignItems: 'center' }}>
                            <span className={`udhr-tag ${imm.status === 'GIVEN' ? 'info' : imm.status === 'MISSED' ? 'danger' : 'info'}`}>{imm.status}</span>
                            {imm.status === 'DUE' && (
                              <>
                                <button type="button" className="udhr-btn-soft-success" onClick={() => handleImmunizationAction(imm.id, 'administer')}>Administer</button>
                                <button type="button" className="udhr-btn-neutral" onClick={() => handleImmunizationAction(imm.id, 'miss')}>Mark missed</button>
                              </>
                            )}
                          </div>
                        </div>
                      ))
                    )}
                  </>
                )}

                {activeRecordTab === 'timeline' && (
                  patientTimeline ? (
                    <>
                      {patientTimeline.activeAlerts && patientTimeline.activeAlerts.length > 0 && (
                        <div style={{ marginBottom: '16px' }}>
                          {patientTimeline.activeAlerts.map(alert => (
                            <div key={alert.id} className="udhr-result-card danger">
                              <p style={{ margin: 0, fontSize: '13px', fontWeight: 600, color: 'var(--udhr-danger-text-2)' }}>Clinical alert: {alert.alertType}</p>
                              <p style={{ margin: '2px 0 0', fontSize: '13px' }}>{alert.message}</p>
                            </div>
                          ))}
                        </div>
                      )}
                      {patientTimeline.foodConflicts && patientTimeline.foodConflicts.length > 0 && (
                        <div className="udhr-result-card warning">
                          <p style={{ margin: 0, fontSize: '13px', fontWeight: 600 }}>Drug-food interactions detected</p>
                          {patientTimeline.foodConflicts.map((c, i) => (
                            <p key={i} style={{ margin: '4px 0 0', fontSize: '12.5px' }}>{c.medication} conflicts with scanned ingredient {c.ingredient} ({c.message})</p>
                          ))}
                        </div>
                      )}
                      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginTop: '12px' }}>
                        <div>
                          <h4 style={{ fontSize: '13px', fontWeight: 700, margin: '0 0 8px' }}>Medication doses (14 days)</h4>
                          {patientTimeline.adherenceLogs.length === 0 ? (
                            <p className="udhr-empty-note">No doses logged.</p>
                          ) : (
                            patientTimeline.adherenceLogs.map(log => (
                              <div key={log.id} className="udhr-list-row" style={{ flexDirection: 'column', alignItems: 'flex-start', padding: '8px 0' }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', width: '100%', fontSize: '12.5px' }}>
                                  <span>{log.reminder.prescription.medication}</span>
                                  <span className={`udhr-tag ${log.status === 'TAKEN' ? 'info' : log.status === 'MISSED' ? 'danger' : 'info'}`}>{log.status}</span>
                                </div>
                                {log.notes && <p style={{ fontSize: '12px', fontStyle: 'italic', margin: '4px 0 0' }}>"{log.notes}"</p>}
                              </div>
                            ))
                          )}
                        </div>
                        <div>
                          <h4 style={{ fontSize: '13px', fontWeight: 700, margin: '0 0 8px' }}>Symptom checks (14 days)</h4>
                          {patientTimeline.symptomChecks.length === 0 ? (
                            <p className="udhr-empty-note">No symptom checks logged.</p>
                          ) : (
                            patientTimeline.symptomChecks.map(check => (
                              <div key={check.id} className="udhr-list-row" style={{ flexDirection: 'column', alignItems: 'flex-start', padding: '8px 0' }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', width: '100%', fontSize: '12.5px' }}>
                                  <span className="udhr-row-subtitle">{new Date(check.checkedAt).toLocaleDateString()}</span>
                                  <span className={`udhr-tag ${check.urgencyLevel === 'RED' ? 'danger' : 'info'}`}>{check.urgencyLevel}</span>
                                </div>
                                <p style={{ margin: '4px 0 0', fontSize: '12.5px' }}>{check.recommendation.split('[')[0]}</p>
                              </div>
                            ))
                          )}
                        </div>
                      </div>
                    </>
                  ) : (
                    <p className="udhr-empty-note">Click "Analyze response (CDS)" above to generate this view.</p>
                  )
                )}

                {activeRecordTab === 'add' && (
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '20px' }}>
                    <form onSubmit={handleAddDiagnosis}>
                      <h4 style={{ fontSize: '13px', fontWeight: 700, margin: '0 0 10px' }}>Add clinical diagnosis</h4>
                      <div className="udhr-form-group">
                        <input type="text" className="udhr-input" placeholder="Condition or diagnosis (e.g. Influenza)" value={addDiagnosisForm.conditionName} onChange={(e) => setAddDiagnosisForm({ ...addDiagnosisForm, conditionName: e.target.value })} required />
                      </div>
                      <div className="udhr-form-group">
                        <textarea className="udhr-textarea" rows={3} placeholder="Clinical notes" value={addDiagnosisForm.notes} onChange={(e) => setAddDiagnosisForm({ ...addDiagnosisForm, notes: e.target.value })} />
                      </div>
                      <button type="submit" className="udhr-btn-neutral">Save diagnosis</button>
                    </form>

                    <form onSubmit={handleAddPrescription}>
                      <h4 style={{ fontSize: '13px', fontWeight: 700, margin: '0 0 10px' }}>Issue prescription</h4>
                      <div className="udhr-form-group">
                        <label className="udhr-label">Medication</label>
                        <input type="text" className="udhr-input" placeholder="e.g. Amoxicillin 250mg" value={addPrescriptionForm.medicationName} onChange={(e) => setAddPrescriptionForm({ ...addPrescriptionForm, medicationName: e.target.value })} required />
                      </div>
                      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px', marginBottom: '12px' }}>
                        <div className="udhr-form-group" style={{ marginBottom: 0 }}>
                          <label className="udhr-label">Dosage</label>
                          <input type="text" className="udhr-input" placeholder="e.g. 1 tablet" value={addPrescriptionForm.dosage} onChange={(e) => setAddPrescriptionForm({ ...addPrescriptionForm, dosage: e.target.value })} required />
                        </div>
                        <div className="udhr-form-group" style={{ marginBottom: 0 }}>
                          <label className="udhr-label">Frequency</label>
                          <select className="udhr-input" value={addPrescriptionForm.frequency} onChange={(e) => setAddPrescriptionForm({ ...addPrescriptionForm, frequency: e.target.value })} required>
                            <option value="Once daily">Once daily</option>
                            <option value="Twice daily">Twice daily</option>
                            <option value="Three times daily">Three times daily</option>
                            <option value="With meals">With meals</option>
                          </select>
                        </div>
                      </div>
                      <div className="udhr-form-group">
                        <label className="udhr-label">Duration (days)</label>
                        <input type="number" className="udhr-input" placeholder="Duration (days)" value={addPrescriptionForm.durationDays} onChange={(e) => setAddPrescriptionForm({ ...addPrescriptionForm, durationDays: parseInt(e.target.value) || 7 })} required />
                      </div>
                      <div className="udhr-form-group">
                        <label className="udhr-label">Dispensing</label>
                        <select className="udhr-input" value={addPrescriptionForm.dispenseMethod} onChange={(e) => setAddPrescriptionForm({ ...addPrescriptionForm, dispenseMethod: e.target.value })} required>
                          <option value="PHARMACY">Send to pharmacy</option>
                          <option value="SELF">Give to patient myself</option>
                        </select>
                      </div>
                      <button type="submit" className="udhr-btn-neutral">{addPrescriptionForm.dispenseMethod === 'SELF' ? 'Issue & dispense now' : 'Send to pharmacy'}</button>
                    </form>

                    <form onSubmit={handleAddAlert}>
                      <h4 style={{ fontSize: '13px', fontWeight: 700, margin: '0 0 10px' }}>Add clinical alert</h4>
                      <div className="udhr-form-group">
                        <select className="udhr-input" value={addAlertForm.severity} onChange={(e) => setAddAlertForm({ ...addAlertForm, severity: e.target.value })} required>
                          <option value="LOW">Low</option>
                          <option value="MEDIUM">Medium</option>
                          <option value="HIGH">High</option>
                          <option value="CRITICAL">Critical</option>
                        </select>
                      </div>
                      <div className="udhr-form-group">
                        <textarea className="udhr-textarea" rows={4} placeholder="Alert message / instruction" value={addAlertForm.message} onChange={(e) => setAddAlertForm({ ...addAlertForm, message: e.target.value })} required />
                      </div>
                      <button type="submit" className="udhr-btn-neutral">Save clinical alert</button>
                    </form>
                  </div>
                )}
              </div>
            </div>
          </>
        ) : null}
      </>
    );

    const renderAlertsPanel = () => (
      <>
        <h1 className="udhr-page-title">Clinical Alerts</h1>
        <p className="udhr-page-subtitle">
          Automatically fired when a patient shows high medication adherence with poor clinical response, or high-risk drug-food interactions.
        </p>

        {clinicalAlerts.length === 0 ? (
          <div className="udhr-empty-state">
            <CheckCircle size={48} color="#86efac" style={{ margin: '0 auto 16px' }} />
            <h3>No active clinical alerts</h3>
            <p className="udhr-empty-note">All monitored patients are responding well to treatment and have no dietary conflicts.</p>
          </div>
        ) : (
          clinicalAlerts.map((item) => {
            const alert = item.alert;
            return (
              <div key={alert.id} className={`udhr-alert-item ${alert.severity === 'CRITICAL' || alert.severity === 'HIGH' ? 'high' : 'medium'}`} style={{ flexDirection: 'column', alignItems: 'stretch' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '12px', borderBottom: '1px solid var(--udhr-border-subtle)', paddingBottom: '12px', marginBottom: '12px' }}>
                  <div>
                    <span className={`udhr-tag ${alert.severity === 'CRITICAL' ? 'danger' : 'info'}`}>{alert.severity} SEVERITY</span>
                    <p className="udhr-row-title" style={{ fontSize: '14px', marginTop: '6px' }}>
                      Patient: {item.patient.firstName} {item.patient.lastName} ({item.patient.idNumber || item.patient.mrn})
                    </p>
                    <p className="udhr-row-subtitle">Fired: {new Date(alert.createdAt).toLocaleString()} · Type: {alert.alertType}</p>
                  </div>
                  <button type="button" className="udhr-btn-soft-success" onClick={() => handleResolveAlert(alert.id)}>Resolve alert</button>
                </div>

                <p style={{ fontSize: '13.5px', lineHeight: 1.6, margin: '0 0 14px', fontStyle: 'italic', background: '#f8fafc', padding: '10px 12px', borderRadius: '8px' }}>
                  {alert.message}
                </p>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', marginBottom: '12px' }}>
                  <div>
                    <h4 style={{ fontSize: '12px', fontWeight: 700, color: 'var(--udhr-text-muted)', margin: '0 0 8px' }}>Adherence & prescriptions</h4>
                    <p style={{ fontSize: '13px', margin: 0 }}>Compliance (last 14 days): <strong>{item.adherenceScore}%</strong></p>
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px', marginTop: '8px' }}>
                      {item.activePrescriptions.map(p => (
                        <span key={p.id} className="udhr-tag info">{p.medication} ({p.dosage})</span>
                      ))}
                    </div>
                  </div>
                  <div>
                    <h4 style={{ fontSize: '12px', fontWeight: 700, color: 'var(--udhr-text-muted)', margin: '0 0 8px' }}>Recent symptom checks</h4>
                    {item.recentSymptomChecks.length === 0 ? (
                      <p className="udhr-empty-note">No checks logged.</p>
                    ) : (
                      item.recentSymptomChecks.map(check => (
                        <p key={check.id} style={{ fontSize: '12.5px', margin: '0 0 4px' }}>
                          {new Date(check.checkedAt).toLocaleDateString()}: urgency <strong>{check.urgencyLevel}</strong>
                        </p>
                      ))
                    )}
                  </div>
                </div>

                {alert.alertType === 'NON_RESPONSE' && (
                  <div style={{ background: '#f8fafc', borderRadius: '10px', padding: '14px', border: '1px solid var(--udhr-border)' }}>
                    <h4 style={{ fontSize: '13px', fontWeight: 700, margin: '0 0 10px' }}>Clinical decision support recommendations</h4>
                    {item.labRecommendations && item.labRecommendations.length > 0 && (
                      <div style={{ marginBottom: '10px', paddingBottom: '10px', borderBottom: '1px dashed var(--udhr-border)' }}>
                        <p style={{ fontSize: '12.5px', fontWeight: 600, margin: '0 0 4px' }}>Suggested laboratory diagnostics:</p>
                        {item.labRecommendations.map(lr => (
                          <div key={lr.id} style={{ marginTop: '4px' }}>
                            <p style={{ fontSize: '12.5px', color: 'var(--udhr-primary)', margin: 0 }}>Order: <strong>{lr.testName}</strong> {lr.icdCode && `(ICD-10: ${lr.icdCode})`}</p>
                            <p className="udhr-row-subtitle">{lr.reason}</p>
                          </div>
                        ))}
                      </div>
                    )}
                    {item.differentialDiagnoses && item.differentialDiagnoses.length > 0 && (
                      <>
                        <p style={{ fontSize: '12.5px', fontWeight: 600, margin: '0 0 6px' }}>Suggested ICD-10 differential diagnoses:</p>
                        {item.differentialDiagnoses.map(dd => (
                          <div key={dd.id} style={{ background: '#fff', padding: '10px', borderRadius: '8px', marginBottom: '6px', border: '1px solid var(--udhr-border)' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                              <span style={{ fontSize: '12.5px', fontWeight: 600 }}>{dd.conditionName} (ICD-10: {dd.icdCode})</span>
                              <span className={`udhr-tag ${dd.likelihood === 'HIGH' ? 'danger' : 'info'}`}>{dd.likelihood}</span>
                            </div>
                            <p className="udhr-row-subtitle">{dd.reasoning}</p>
                          </div>
                        ))}
                      </>
                    )}
                  </div>
                )}
              </div>
            );
          })
        )}
      </>
    );

    return (
      <div className="udhr-shell">
        {isMobileNavOpen && <div className="udhr-shell-backdrop" onClick={() => setIsMobileNavOpen(false)} />}
        <aside className={`udhr-sidebar ${isMobileNavOpen ? 'open' : ''}`}>
          <div className="udhr-sidebar-header">
            <div className="udhr-page-logo">
              <div className="udhr-page-logo-chip"><Activity size={16} color="#fff" /></div>
              UDHR
            </div>
            <button type="button" className="udhr-sidebar-close" onClick={() => setIsMobileNavOpen(false)}><XCircle size={18} /></button>
          </div>
          <nav className="udhr-sidebar-nav">
            {clinicalNavItems.map(item => (
              <button key={item.key} type="button" className={`udhr-nav-item ${activeTabStaff === item.key ? 'active' : ''}`} onClick={() => selectNavTab(item.key)}>
                {item.icon} {item.label}
                {item.key === 'alerts' && clinicalAlerts.length > 0 && <span className="udhr-nav-badge">{clinicalAlerts.length}</span>}
              </button>
            ))}
          </nav>
          <div className="udhr-sidebar-footer">
            <div className="udhr-user-row">
              <div className="udhr-user-avatar">{userName ? userName.charAt(0) : <User size={14} />}</div>
              <div style={{ minWidth: 0 }}>
                <p className="udhr-user-name">{userName}</p>
                <p className="udhr-user-role">{userRole}</p>
              </div>
            </div>
            <button type="button" className="udhr-logout-link" onClick={handleLogout}><LogOut size={15} /> Log out</button>
          </div>
        </aside>

        <main className="udhr-shell-main">
          <button type="button" className="udhr-mobile-menu-btn" onClick={() => setIsMobileNavOpen(true)}>
            <Menu size={16} /> Menu
          </button>

          {errorMessage && (
            <div className="udhr-alert-banner error">
              <AlertCircle size={16} />
              <span style={{ flex: 1 }}>{errorMessage}</span>
              <button type="button" onClick={() => setErrorMessage('')}>×</button>
            </div>
          )}
          {successMessage && (
            <div className="udhr-alert-banner success">
              <CheckCircle size={16} />
              <span style={{ flex: 1 }}>{successMessage}</span>
              <button type="button" onClick={() => setSuccessMessage('')}>×</button>
            </div>
          )}

          {activeTabStaff === 'patients' && (isNurse ? renderNurseRegisterPanel() : renderDoctorPatientsPanel())}
          {activeTabStaff === 'vitals' && isNurse && renderNurseVitalsPanel()}
          {activeTabStaff === 'labs' && isNurse && renderNurseLabsPanel()}
          {activeTabStaff === 'alerts' && renderAlertsPanel()}
          {activeTabStaff === 'queue' && renderQueuePanel()}
          {activeTabStaff === 'referrals' && renderReferralsPanel()}
        </main>
      </div>
    );
  }

  // 4. Pharmacist dashboard — light clinical redesign, same sidebar shell as
  // Clinical (Dispense/Stock nav). Reuses all existing state and handlers.
  if (token && isPharmacist && !mustChangePassword) {
    const pharmNavItems = [
      { key: 'dispense', label: 'Dispense', icon: <Pill size={17} /> },
      { key: 'stock', label: 'Stock', icon: <Package size={17} /> },
    ];

    const selectPharmNavTab = (key) => {
      setPharmacistTab(key);
      setIsMobileNavOpen(false);
      if (key === 'stock') fetchStockItems();
      if (key === 'dispense') fetchPharmacyQueue();
    };

    const renderDispensePanel = () => (
      <>
        <h1 className="udhr-page-title">Dispense</h1>
        <p className="udhr-page-subtitle">Patients the doctor has just prescribed for — pick one from the list below.</p>

        {!pharmacyRecord && (
          <>
            <p className="udhr-label" style={{ marginBottom: '8px' }}>Look up by ID/MRN (e.g. a self-pay walk-in)</p>
            <form onSubmit={handleSearchPharmacyPatient} className="udhr-search-bar" style={{ marginBottom: '20px' }}>
              <input
                type="text"
                className="udhr-input"
                value={pharmacySearchId}
                onChange={(e) => setPharmacySearchId(e.target.value)}
                placeholder="Enter patient ID number or MRN"
                required
              />
              <button type="submit" className="udhr-btn-compact" disabled={loading}>{loading ? 'Searching...' : 'Search'}</button>
            </form>

            <p className="udhr-label" style={{ marginBottom: '8px' }}>Or pick from the queue</p>
            <div className="udhr-record-card">
              <div className="udhr-record-body">
                {pharmacyQueue.length === 0 ? (
                  <p className="udhr-empty-note">Nothing waiting to be dispensed right now.</p>
                ) : (
                  pharmacyQueue.map(item => (
                    <div key={item.currentVisit.id} className="udhr-list-row">
                      <div>
                        <p className="udhr-row-title">{item.patient.firstName} {item.patient.lastName}</p>
                        <p className="udhr-row-subtitle">
                          {item.patient.idNumber ? `ID: ${item.patient.idNumber} · ` : ''}MRN: {item.patient.mrn}
                        </p>
                        <p className="udhr-row-subtitle">{item.pendingPrescriptions.length} prescription{item.pendingPrescriptions.length === 1 ? '' : 's'} waiting · from {item.currentVisit.staff ? `${item.currentVisit.staff.firstName} ${item.currentVisit.staff.lastName}` : 'doctor'}</p>
                      </div>
                      <button type="button" className="udhr-btn-primary" onClick={() => handleSelectFromPharmacyQueue(item)} disabled={loading}>View & dispense</button>
                    </div>
                  ))
                )}
              </div>
            </div>
          </>
        )}

        {pharmacyRecord ? (
          <>
            <button type="button" className="udhr-btn-neutral" onClick={handleBackToPharmacyQueue} style={{ marginBottom: '16px' }}>← Back to queue</button>
            <div className="udhr-record-card" style={{ marginBottom: '20px' }}>
              <div className="udhr-record-body">
                <p className="udhr-row-title" style={{ fontSize: '15px' }}>{pharmacyRecord.patient.firstName} {pharmacyRecord.patient.lastName}</p>
                <p className="udhr-row-subtitle">{pharmacyRecord.patient.idNumber ? `ID: ${pharmacyRecord.patient.idNumber} · ` : ''}MRN: {pharmacyRecord.patient.mrn}</p>
                {pharmacyRecord.currentVisit ? (
                  <p className="udhr-row-subtitle" style={{ marginTop: '8px' }}>
                    Coming from: {pharmacyRecord.currentVisit.staff ? `${pharmacyRecord.currentVisit.staff.firstName} ${pharmacyRecord.currentVisit.staff.lastName} (${pharmacyRecord.currentVisit.staff.role})` : 'Unknown staff member'} — {pharmacyRecord.currentVisit.reason}
                    <br />Visit status: <span className="udhr-tag info" style={{ marginLeft: '4px' }}>{pharmacyRecord.currentVisit.status}</span>
                  </p>
                ) : (
                  <p className="udhr-row-subtitle" style={{ marginTop: '8px' }}>No visit on file for this patient yet.</p>
                )}
              </div>
            </div>

            <div className="udhr-record-card" style={{ marginBottom: '20px' }}>
              <div className="udhr-record-body">
                <h3 style={{ fontSize: '14px', fontWeight: 700, margin: '0 0 14px' }}>Prescriptions waiting to be dispensed</h3>
                {pharmacyRecord.pendingPrescriptions.length === 0 ? (
                  <div style={{ textAlign: 'center', padding: '30px 20px' }}>
                    <CheckCircle size={36} color="#86efac" style={{ margin: '0 auto 10px' }} />
                    <p className="udhr-empty-note">Nothing waiting for this patient at the pharmacy right now.</p>
                  </div>
                ) : (
                  pharmacyRecord.pendingPrescriptions.map(p => (
                    <div key={p.id} className="udhr-list-row">
                      <div>
                        <p className="udhr-row-title">{p.medication}</p>
                        <p className="udhr-row-subtitle">{p.dosage} · {p.frequency}</p>
                        <p className="udhr-row-subtitle">Prescribed by {p.doctor ? `${p.doctor.firstName} ${p.doctor.lastName}` : 'doctor'} on {new Date(p.createdAt).toLocaleDateString()}</p>
                        {p.notes && <p className="udhr-row-subtitle" style={{ fontStyle: 'italic' }}>{p.notes}</p>}
                      </div>
                      <button
                        type="button"
                        onClick={() => handleDispense(p.id)}
                        style={{ padding: '8px 14px', border: 'none', borderRadius: '8px', background: 'var(--udhr-success)', color: '#fff', fontSize: '12.5px', fontWeight: 600, cursor: 'pointer', whiteSpace: 'nowrap', flexShrink: 0 }}
                      >
                        Mark dispensed
                      </button>
                    </div>
                  ))
                )}
              </div>
            </div>

            <div className="udhr-record-card">
              <div className="udhr-record-body">
                <h3 style={{ fontSize: '14px', fontWeight: 700, margin: '0 0 14px' }}>Log dispense event</h3>
                <form onSubmit={handleLogDispense} style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                  <input type="number" className="udhr-input" placeholder="Prescription ID" value={dispenseLogForm.prescriptionId} onChange={(e) => setDispenseLogForm({ ...dispenseLogForm, prescriptionId: e.target.value })} required />
                  <input type="text" className="udhr-input" placeholder="Quantity dispensed (e.g. 30 tablets)" value={dispenseLogForm.quantityDispensed} onChange={(e) => setDispenseLogForm({ ...dispenseLogForm, quantityDispensed: e.target.value })} required />
                  <input type="number" className="udhr-input" placeholder="Days supply" value={dispenseLogForm.daysSupply} onChange={(e) => setDispenseLogForm({ ...dispenseLogForm, daysSupply: e.target.value })} />
                  <input type="text" className="udhr-input" placeholder="Pharmacy notes" value={dispenseLogForm.pharmacyNotes} onChange={(e) => setDispenseLogForm({ ...dispenseLogForm, pharmacyNotes: e.target.value })} />
                  <button type="submit" className="udhr-btn-neutral" style={{ gridColumn: '1 / -1' }}>Log dispense</button>
                </form>

                {dispenseHistory.length > 0 && (
                  <div style={{ marginTop: '18px', paddingTop: '14px', borderTop: '1px dashed var(--udhr-border)' }}>
                    <p style={{ fontSize: '12px', fontWeight: 700, color: 'var(--udhr-text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em', margin: '0 0 10px' }}>Dispense history</p>
                    {dispenseHistory.map(d => (
                      <div key={d.id} className="udhr-list-row">
                        <p className="udhr-row-title" style={{ fontSize: '13px' }}>{d.prescription.medication} — {d.quantityDispensed}{d.daysSupply ? ` (${d.daysSupply} days)` : ''}</p>
                        <p className="udhr-row-subtitle">by {d.dispensedBy.firstName} {d.dispensedBy.lastName} on {new Date(d.dispensedAt).toLocaleDateString()}</p>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </>
        ) : null}
      </>
    );

    const renderStockPanel = () => (
      <>
        <h1 className="udhr-page-title">Stock</h1>
        <p className="udhr-page-subtitle">Inventory levels across the pharmacy.</p>

        <div className="udhr-record-card" style={{ marginBottom: '20px', maxWidth: '640px' }}>
          <div className="udhr-record-body">
            <h3 style={{ fontSize: '14px', fontWeight: 700, margin: '0 0 14px' }}>Add stock item</h3>
            <form onSubmit={handleAddStockItem} style={{ display: 'grid', gridTemplateColumns: '2fr 1fr 1fr 1fr', gap: '10px', alignItems: 'flex-end' }}>
              <div className="udhr-form-group" style={{ marginBottom: 0 }}>
                <label className="udhr-label">Medication name</label>
                <input type="text" className="udhr-input" value={newStockItemForm.medicationName} onChange={(e) => setNewStockItemForm({ ...newStockItemForm, medicationName: e.target.value })} required />
              </div>
              <div className="udhr-form-group" style={{ marginBottom: 0 }}>
                <label className="udhr-label">Unit</label>
                <input type="text" className="udhr-input" value={newStockItemForm.unit} onChange={(e) => setNewStockItemForm({ ...newStockItemForm, unit: e.target.value })} />
              </div>
              <div className="udhr-form-group" style={{ marginBottom: 0 }}>
                <label className="udhr-label">Quantity</label>
                <input type="number" className="udhr-input" value={newStockItemForm.quantityOnHand} onChange={(e) => setNewStockItemForm({ ...newStockItemForm, quantityOnHand: parseInt(e.target.value, 10) || 0 })} />
              </div>
              <div className="udhr-form-group" style={{ marginBottom: 0 }}>
                <label className="udhr-label">Reorder level</label>
                <input type="number" className="udhr-input" value={newStockItemForm.reorderLevel} onChange={(e) => setNewStockItemForm({ ...newStockItemForm, reorderLevel: parseInt(e.target.value, 10) || 0 })} />
              </div>
              <button type="submit" className="udhr-btn-neutral" style={{ gridColumn: '1 / -1' }}>Add item</button>
            </form>
          </div>
        </div>

        <div className="udhr-record-card">
          <div className="udhr-record-body">
            {stockItems.length === 0 ? (
              <p className="udhr-empty-note">No stock items tracked yet. Add one above.</p>
            ) : (
              stockItems.map(item => (
                <div key={item.id} className="udhr-list-row">
                  <div>
                    <p className="udhr-row-title">
                      {item.medicationName}{' '}
                      {item.quantityOnHand <= item.reorderLevel && (
                        <span className="udhr-tag danger" style={{ marginLeft: '6px' }}>LOW STOCK</span>
                      )}
                    </p>
                    <p className="udhr-row-subtitle">{item.quantityOnHand} {item.unit} on hand — reorder below {item.reorderLevel}</p>
                  </div>
                  <div style={{ display: 'flex', gap: '6px', alignItems: 'center' }}>
                    <input
                      type="number"
                      className="udhr-input"
                      placeholder="±qty"
                      value={stockAdjustAmount[item.id] || ''}
                      onChange={(e) => setStockAdjustAmount(prev => ({ ...prev, [item.id]: e.target.value }))}
                      style={{ width: '90px' }}
                    />
                    <button type="button" className="udhr-btn-neutral" onClick={() => handleAdjustStock(item.id)}>Adjust</button>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </>
    );

    return (
      <div className="udhr-shell">
        {isMobileNavOpen && <div className="udhr-shell-backdrop" onClick={() => setIsMobileNavOpen(false)} />}
        <aside className={`udhr-sidebar ${isMobileNavOpen ? 'open' : ''}`}>
          <div className="udhr-sidebar-header">
            <div className="udhr-page-logo">
              <div className="udhr-page-logo-chip"><Activity size={16} color="#fff" /></div>
              UDHR
            </div>
            <button type="button" className="udhr-sidebar-close" onClick={() => setIsMobileNavOpen(false)}><XCircle size={18} /></button>
          </div>
          <nav className="udhr-sidebar-nav">
            {pharmNavItems.map(item => (
              <button key={item.key} type="button" className={`udhr-nav-item ${pharmacistTab === item.key ? 'active' : ''}`} onClick={() => selectPharmNavTab(item.key)}>
                {item.icon} {item.label}
              </button>
            ))}
          </nav>
          <div className="udhr-sidebar-footer">
            <div className="udhr-user-row">
              <div className="udhr-user-avatar">{userName ? userName.charAt(0) : <User size={14} />}</div>
              <div style={{ minWidth: 0 }}>
                <p className="udhr-user-name">{userName}</p>
                <p className="udhr-user-role">{userRole}</p>
              </div>
            </div>
            <button type="button" className="udhr-logout-link" onClick={handleLogout}><LogOut size={15} /> Log out</button>
          </div>
        </aside>

        <main className="udhr-shell-main">
          <button type="button" className="udhr-mobile-menu-btn" onClick={() => setIsMobileNavOpen(true)}>
            <Menu size={16} /> Menu
          </button>

          {errorMessage && (
            <div className="udhr-alert-banner error">
              <AlertCircle size={16} />
              <span style={{ flex: 1 }}>{errorMessage}</span>
              <button type="button" onClick={() => setErrorMessage('')}>×</button>
            </div>
          )}
          {successMessage && (
            <div className="udhr-alert-banner success">
              <CheckCircle size={16} />
              <span style={{ flex: 1 }}>{successMessage}</span>
              <button type="button" onClick={() => setSuccessMessage('')}>×</button>
            </div>
          )}

          {pharmacistTab === 'dispense' ? renderDispensePanel() : renderStockPanel()}
        </main>
      </div>
    );
  }

  // 5. Admin dashboard — light clinical redesign, same sidebar shell
  // (Front Desk/Staff/Facilities nav). Reuses all existing state and
  // handlers. The mockup's "Announcements" nav item is skipped — there's
  // no announcements feature (model, endpoints, or state) anywhere in this
  // app to redesign; adding one would be new functionality, not a
  // presentation change.
  if (token && isAdmin && !mustChangePassword) {
    const adminNavItems = [
      { key: 'frontdesk', label: 'Front Desk', icon: <Clipboard size={17} /> },
      { key: 'visitors', label: 'Visitors', icon: <Clock size={17} /> },
      { key: 'staff', label: 'Staff', icon: <Users size={17} /> },
      { key: 'facilities', label: 'Facilities', icon: <Building2 size={17} /> },
      { key: 'referrals', label: 'Referrals', icon: <CornerUpRight size={17} /> },
      { key: 'announcements', label: 'Announcements', icon: <Megaphone size={17} /> },
    ];

    const selectAdminNavTab = (key) => {
      setAdminTab(key);
      setIsMobileNavOpen(false);
      if (key === 'visitors') fetchRecentCheckIns();
      if (key === 'staff') fetchStaffList();
      if (key === 'facilities') fetchFacilityList();
      if (key === 'referrals') fetchReferralReport();
      if (key === 'announcements') fetchAnnouncements();
    };

    const renderFrontDeskPanel = () => (
      <>
        <h1 className="udhr-page-title">Front Desk</h1>
        <p className="udhr-page-subtitle">Register new patients and check existing ones in on arrival — see who's been in under Visitors.</p>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px,1fr))', gap: '20px', marginBottom: '20px' }}>
          <div className="udhr-record-card">
            <div className="udhr-record-body">
              <h3 style={{ fontSize: '14px', fontWeight: 700, margin: '0 0 14px' }}>Register new patient</h3>
              <form onSubmit={handleRegisterPatient}>
                <div className="udhr-form-group">
                  <label className="udhr-label">ID Number</label>
                  <input type="text" className="udhr-input" value={patientRegForm.idNumber} onChange={(e) => setPatientRegForm({ ...patientRegForm, idNumber: e.target.value.replace(/\D/g, '').slice(0, 13) })} placeholder="SA ID number (optional)" maxLength={13} inputMode="numeric" pattern="[0-9]*" />
                </div>
                <div className="udhr-form-group">
                  <label className="udhr-label">Passport Number</label>
                  <input type="text" className="udhr-input" value={patientRegForm.passportNumber} onChange={(e) => setPatientRegForm({ ...patientRegForm, passportNumber: e.target.value })} placeholder="Passport number (optional)" />
                </div>
                <p className="udhr-empty-note" style={{ marginBottom: '12px' }}>
                  An MRN is auto-assigned to every patient regardless — ID number and passport number are optional.
                </p>
                <div className="udhr-form-group">
                  <label className="udhr-label">First Name</label>
                  <input type="text" className="udhr-input" value={patientRegForm.firstName} onChange={(e) => setPatientRegForm({ ...patientRegForm, firstName: e.target.value })} required />
                </div>
                <div className="udhr-form-group">
                  <label className="udhr-label">Last Name</label>
                  <input type="text" className="udhr-input" value={patientRegForm.lastName} onChange={(e) => setPatientRegForm({ ...patientRegForm, lastName: e.target.value })} required />
                </div>
                <div className="udhr-form-group">
                  <label className="udhr-label">Date of Birth</label>
                  <input type="date" className="udhr-input" value={patientRegForm.dateOfBirth} onChange={(e) => setPatientRegForm({ ...patientRegForm, dateOfBirth: e.target.value })} required max={new Date().toISOString().split('T')[0]} />
                </div>
                <div className="udhr-form-group">
                  <label className="udhr-label">Gender</label>
                  <select className="udhr-input" value={patientRegForm.gender} onChange={(e) => setPatientRegForm({ ...patientRegForm, gender: e.target.value })} required>
                    <option value="" disabled>Select gender</option>
                    <option value="MALE">Male</option>
                    <option value="FEMALE">Female</option>
                    <option value="OTHER">Other</option>
                  </select>
                </div>
                <div className="udhr-form-group">
                  <label className="udhr-label">Contact Number</label>
                  <input type="text" className="udhr-input" value={patientRegForm.contactNumber} onChange={(e) => setPatientRegForm({ ...patientRegForm, contactNumber: e.target.value.replace(/\D/g, '') })} inputMode="numeric" pattern="[0-9]*" />
                </div>
                <div className="udhr-form-group">
                  <label className="udhr-label">Email Address</label>
                  <input type="email" className="udhr-input" value={patientRegForm.email} onChange={(e) => setPatientRegForm({ ...patientRegForm, email: e.target.value })} placeholder="patient@gmail.com" />
                </div>
                <div className="udhr-form-group">
                  <label className="udhr-label">Address</label>
                  <textarea className="udhr-textarea" value={patientRegForm.address} onChange={(e) => setPatientRegForm({ ...patientRegForm, address: e.target.value })} rows={2} />
                </div>
                <div className="udhr-form-group">
                  <label className="udhr-label">Reason for visit (optional)</label>
                  <input type="text" className="udhr-input" value={regReasonForVisit} onChange={(e) => setRegReasonForVisit(e.target.value)} placeholder="e.g. Follow-up, flu symptoms..." />
                  <p className="udhr-empty-note" style={{ marginTop: '4px' }}>
                    Fill this in to check the patient in and queue them for vitals right away — leave blank to just register them.
                  </p>
                </div>
                <button
                  type="button"
                  className="udhr-btn-neutral"
                  style={{ width: '100%', marginBottom: '10px' }}
                  onClick={() => setShowRegExtras(!showRegExtras)}
                >
                  {showRegExtras ? 'Hide' : 'Add'} Next of Kin / Newborn Details (optional)
                </button>
                {showRegExtras && (
                  <div style={{ background: '#f8fafc', border: '1px solid var(--udhr-border)', padding: '12px', borderRadius: '8px', marginBottom: '10px' }}>
                    <p className="udhr-empty-note" style={{ marginBottom: '8px' }}>Next of Kin</p>
                    <div className="udhr-form-group"><input type="text" className="udhr-input" placeholder="First name" value={patientRegForm.nextOfKinFirstName} onChange={(e) => setPatientRegForm({ ...patientRegForm, nextOfKinFirstName: e.target.value })} /></div>
                    <div className="udhr-form-group"><input type="text" className="udhr-input" placeholder="Last name" value={patientRegForm.nextOfKinLastName} onChange={(e) => setPatientRegForm({ ...patientRegForm, nextOfKinLastName: e.target.value })} /></div>
                    <div className="udhr-form-group"><input type="text" className="udhr-input" placeholder="Relationship (e.g. Husband)" value={patientRegForm.nextOfKinRelationship} onChange={(e) => setPatientRegForm({ ...patientRegForm, nextOfKinRelationship: e.target.value })} /></div>
                    <div className="udhr-form-group"><input type="text" className="udhr-input" placeholder="Phone number" value={patientRegForm.nextOfKinPhone} onChange={(e) => setPatientRegForm({ ...patientRegForm, nextOfKinPhone: e.target.value })} /></div>
                    <p className="udhr-empty-note" style={{ margin: '12px 0 8px' }}>Newborn — leave blank unless registering a baby at birth</p>
                    <div className="udhr-form-group"><input type="text" className="udhr-input" placeholder="Mother's ID number" value={patientRegForm.motherIdNumber} onChange={(e) => setPatientRegForm({ ...patientRegForm, motherIdNumber: e.target.value })} /></div>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
                      <input type="number" className="udhr-input" placeholder="Birth weight (g)" value={patientRegForm.birthWeightGrams} onChange={(e) => setPatientRegForm({ ...patientRegForm, birthWeightGrams: e.target.value })} />
                      <input type="number" className="udhr-input" placeholder="Birth length (cm)" value={patientRegForm.birthLengthCm} onChange={(e) => setPatientRegForm({ ...patientRegForm, birthLengthCm: e.target.value })} />
                      <input type="number" className="udhr-input" placeholder="Apgar (1 min)" value={patientRegForm.apgarScore1Min} onChange={(e) => setPatientRegForm({ ...patientRegForm, apgarScore1Min: e.target.value })} />
                      <input type="number" className="udhr-input" placeholder="Apgar (5 min)" value={patientRegForm.apgarScore5Min} onChange={(e) => setPatientRegForm({ ...patientRegForm, apgarScore5Min: e.target.value })} />
                    </div>
                  </div>
                )}
                <button type="submit" className="udhr-btn-primary">Create patient record</button>
              </form>
            </div>
          </div>

          <div className="udhr-record-card" style={{ alignSelf: 'flex-start' }}>
            <div className="udhr-record-body">
              <h3 style={{ fontSize: '14px', fontWeight: 700, margin: '0 0 6px' }}>Check in patient</h3>
              <p className="udhr-empty-note" style={{ marginBottom: '14px' }}>
                Queues the patient for vitals — they'll appear automatically on the nurse's dashboard, no ID search needed on either side.
              </p>
              <form onSubmit={handleCheckIn}>
                <div className="udhr-form-group">
                  <label className="udhr-label">ID Number or MRN</label>
                  <input
                    type="text"
                    className="udhr-input"
                    value={checkinSearchId}
                    onChange={(e) => setCheckinSearchId(e.target.value)}
                    placeholder="Enter patient ID number or MRN"
                    required
                  />
                </div>
                <div className="udhr-form-group">
                  <label className="udhr-label">Reason for visit (optional)</label>
                  <input
                    type="text"
                    className="udhr-input"
                    value={checkinReason}
                    onChange={(e) => setCheckinReason(e.target.value)}
                    placeholder="e.g. Follow-up, flu symptoms..."
                  />
                </div>
                <button type="submit" className="udhr-btn-primary">Check in</button>
              </form>
            </div>
          </div>
        </div>
      </>
    );

    const renderVisitorsPanel = () => (
      <>
        <h1 className="udhr-page-title">Visitors</h1>
        <p className="udhr-page-subtitle">Who's come through the door today, plus look-up for anyone not on that list.</p>

        <div className="udhr-record-card" style={{ marginBottom: '20px' }}>
          <div className="udhr-record-body">
            <h3 style={{ fontSize: '14px', fontWeight: 700, margin: '0 0 6px' }}>Look up a patient</h3>
            <p className="udhr-empty-note" style={{ marginBottom: '14px' }}>
              For anyone not on today's list — confirms they're registered and shows when they were last seen. Registration and contact details only, no clinical record.
            </p>
            <form onSubmit={handleAdminPatientLookup} className="udhr-search-bar">
              <input
                type="text"
                className="udhr-input"
                value={adminLookupId}
                onChange={(e) => setAdminLookupId(e.target.value)}
                placeholder="Enter patient ID number or MRN"
                required
              />
              <button type="submit" className="udhr-btn-compact" disabled={loading}>{loading ? 'Searching...' : 'Search'}</button>
            </form>

            {adminLookupRecord && (
              <div style={{ marginTop: '16px', paddingTop: '16px', borderTop: '1px dashed var(--udhr-border)' }}>
                <p className="udhr-row-title" style={{ fontSize: '15px' }}>
                  {adminLookupRecord.patient.firstName} {adminLookupRecord.patient.lastName}
                  <span className={`udhr-tag ${adminLookupRecord.firstVisit ? 'info' : 'neutral'}`} style={{ marginLeft: '8px' }}>{adminLookupRecord.firstVisit ? 'NEW' : 'RETURNING'}</span>
                </p>
                <p className="udhr-row-subtitle">
                  {adminLookupRecord.patient.idNumber ? `ID: ${adminLookupRecord.patient.idNumber} · ` : ''}MRN: {adminLookupRecord.patient.mrn} · {adminLookupRecord.patient.gender} · Born {adminLookupRecord.patient.dateOfBirth}
                </p>
                <p className="udhr-row-subtitle" style={{ marginBottom: '12px' }}>
                  Contact: {adminLookupRecord.patient.contactNumber || 'N/A'} · Email: {adminLookupRecord.patient.email || 'N/A'}
                </p>

                <p style={{ fontSize: '12px', fontWeight: 700, color: 'var(--udhr-text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em', margin: '0 0 8px' }}>Visit history</p>
                {adminLookupRecord.visits.length === 0 ? (
                  <p className="udhr-empty-note">No visits on file yet.</p>
                ) : (
                  adminLookupRecord.visits.map(v => (
                    <div key={v.id} className="udhr-list-row">
                      <div>
                        <p className="udhr-row-title">{v.reason}</p>
                        <p className="udhr-row-subtitle">{v.facility?.name || 'Unknown facility'}</p>
                      </div>
                      <div style={{ textAlign: 'right' }}>
                        <span className={`udhr-tag ${v.status === 'COMPLETE' ? 'info' : 'warning'}`}>{v.status.replaceAll('_', ' ')}</span>
                        <p className="udhr-row-subtitle" style={{ marginTop: '2px' }}>{new Date(v.visitDate).toLocaleString()}</p>
                      </div>
                    </div>
                  ))
                )}
              </div>
            )}
          </div>
        </div>

        <p style={{ fontSize: '12px', fontWeight: 700, color: 'var(--udhr-text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em', margin: '0 0 10px' }}>Today's visitors</p>
        <div className="udhr-record-card">
          <div className="udhr-record-body">
            {recentCheckIns.length === 0 ? (
              <p className="udhr-empty-note">No check-ins logged yet today.</p>
            ) : (
              recentCheckIns.map(entry => {
                const v = entry.visit;
                return (
                  <div key={v.id} className="udhr-list-row">
                    <div>
                      <p className="udhr-row-title">
                        {v.patient.firstName} {v.patient.lastName} <span className="udhr-row-subtitle">({v.patient.idNumber || v.patient.mrn})</span>
                        <span className={`udhr-tag ${entry.firstVisit ? 'info' : 'neutral'}`} style={{ marginLeft: '8px' }}>{entry.firstVisit ? 'NEW' : 'RETURNING'}</span>
                      </p>
                      <p className="udhr-row-subtitle">{v.reason}</p>
                    </div>
                    <div style={{ textAlign: 'right' }}>
                      <span className={`udhr-tag ${v.status === 'COMPLETE' ? 'info' : 'warning'}`}>{v.status.replaceAll('_', ' ')}</span>
                      <p className="udhr-row-subtitle" style={{ marginTop: '2px' }}>{new Date(v.visitDate).toLocaleString()}</p>
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>
      </>
    );

    const renderStaffPanel = () => (
      <>
        <h1 className="udhr-page-title">Staff</h1>
        <p className="udhr-page-subtitle">Everyone with access to this facility.</p>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px,1fr))', gap: '20px' }}>
          <div className="udhr-record-card" style={{ alignSelf: 'flex-start' }}>
            <div className="udhr-record-body">
              <h3 style={{ fontSize: '14px', fontWeight: 700, margin: '0 0 14px' }}>Register staff member</h3>
              <form onSubmit={handleRegisterStaff}>
                <div className="udhr-form-group">
                  <label className="udhr-label">Staff Number</label>
                  <input type="text" className="udhr-input" value={newStaffForm.staffNumber} onChange={(e) => setNewStaffForm({ ...newStaffForm, staffNumber: e.target.value })} placeholder="e.g. NUR002" required />
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', marginBottom: '12px' }}>
                  <input type="text" className="udhr-input" placeholder="First name" value={newStaffForm.firstName} onChange={(e) => setNewStaffForm({ ...newStaffForm, firstName: e.target.value })} required />
                  <input type="text" className="udhr-input" placeholder="Last name" value={newStaffForm.lastName} onChange={(e) => setNewStaffForm({ ...newStaffForm, lastName: e.target.value })} required />
                </div>
                <div className="udhr-form-group">
                  <label className="udhr-label">Role</label>
                  <select className="udhr-input" value={newStaffForm.role} onChange={(e) => setNewStaffForm({ ...newStaffForm, role: e.target.value })} required>
                    <option value="DOCTOR">Doctor</option>
                    <option value="NURSE">Nurse</option>
                    <option value="PHARMACIST">Pharmacist</option>
                    <option value="ADMIN">Admin</option>
                  </select>
                </div>
                <div className="udhr-form-group">
                  <label className="udhr-label">Facility</label>
                  <select className="udhr-input" value={newStaffForm.facilityId} onChange={(e) => setNewStaffForm({ ...newStaffForm, facilityId: e.target.value })} required>
                    <option value="">Select a facility...</option>
                    {facilityList.map(f => (
                      <option key={f.id} value={f.id}>{f.name}</option>
                    ))}
                  </select>
                </div>
                <div className="udhr-form-group">
                  <label className="udhr-label">Email</label>
                  <input type="email" className="udhr-input" value={newStaffForm.email} onChange={(e) => setNewStaffForm({ ...newStaffForm, email: e.target.value })} required />
                </div>
                <div className="udhr-form-group">
                  <label className="udhr-label">Temporary Password</label>
                  <div className="udhr-password-field">
                    <input type={showNewStaffPassword ? 'text' : 'password'} className="udhr-input" value={newStaffForm.password} onChange={(e) => setNewStaffForm({ ...newStaffForm, password: e.target.value })} required />
                    <button type="button" className="udhr-password-toggle" onClick={() => setShowNewStaffPassword(!showNewStaffPassword)} aria-label={showNewStaffPassword ? 'Hide password' : 'Show password'}>
                      {showNewStaffPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                    </button>
                  </div>
                </div>
                <button type="submit" className="udhr-btn-primary">Create staff account</button>
              </form>
            </div>
          </div>

          <div className="udhr-record-card" style={{ alignSelf: 'flex-start' }}>
            <div className="udhr-record-body">
              {staffList.length === 0 ? (
                <p className="udhr-empty-note">{loading ? 'Loading...' : 'No staff members yet.'}</p>
              ) : (
                staffList.map(s => (
                  <div key={s.id} className="udhr-list-row">
                    <div>
                      <p className="udhr-row-title">{s.firstName} {s.lastName} <span className="udhr-row-subtitle">({s.role})</span></p>
                      <p className="udhr-row-subtitle">{s.staffNumber} · {s.email}</p>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <span className={`udhr-tag ${s.active ? 'info' : 'danger'}`}>{s.active ? 'Active' : 'Inactive'}</span>
                      {s.active && (
                        <button type="button" className="udhr-btn-neutral" onClick={() => handleDeactivateStaff(s.id)}>Deactivate</button>
                      )}
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      </>
    );

    const renderFacilitiesPanel = () => (
      <>
        <h1 className="udhr-page-title">Facilities</h1>
        <p className="udhr-page-subtitle">Clinics and hospitals in the network.</p>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px,1fr))', gap: '20px' }}>
          <div className="udhr-record-card" style={{ alignSelf: 'flex-start' }}>
            <div className="udhr-record-body">
              <h3 style={{ fontSize: '14px', fontWeight: 700, margin: '0 0 14px' }}>Add facility</h3>
              <form onSubmit={handleAddFacility}>
                <div className="udhr-form-group">
                  <label className="udhr-label">Name</label>
                  <input type="text" className="udhr-input" value={newFacilityForm.name} onChange={(e) => setNewFacilityForm({ ...newFacilityForm, name: e.target.value })} required />
                </div>
                <div className="udhr-form-group">
                  <label className="udhr-label">Type</label>
                  <select className="udhr-input" value={newFacilityForm.type} onChange={(e) => setNewFacilityForm({ ...newFacilityForm, type: e.target.value })}>
                    <option value="CLINIC">Clinic</option>
                    <option value="HOSPITAL">Hospital</option>
                  </select>
                </div>
                <div className="udhr-form-group">
                  <label className="udhr-label">Province</label>
                  <input type="text" className="udhr-input" value={newFacilityForm.province} onChange={(e) => setNewFacilityForm({ ...newFacilityForm, province: e.target.value })} required />
                </div>
                <div className="udhr-form-group">
                  <label className="udhr-label">Address</label>
                  <textarea className="udhr-textarea" value={newFacilityForm.address} onChange={(e) => setNewFacilityForm({ ...newFacilityForm, address: e.target.value })} rows={2} />
                </div>
                <button type="submit" className="udhr-btn-primary">Add facility</button>
              </form>
            </div>
          </div>

          <div className="udhr-record-card" style={{ alignSelf: 'flex-start' }}>
            <div className="udhr-record-body">
              {facilityList.length === 0 ? (
                <p className="udhr-empty-note">{loading ? 'Loading...' : 'No facilities yet.'}</p>
              ) : (
                facilityList.map(f => (
                  <div key={f.id} className="udhr-list-row" style={{ flexDirection: 'column', alignItems: 'flex-start' }}>
                    <p className="udhr-row-title">{f.name} <span className="udhr-row-subtitle">({f.type})</span></p>
                    <p className="udhr-row-subtitle">{f.province} — {f.address}</p>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      </>
    );

    const renderReferralsPanel = () => (
      <>
        <h1 className="udhr-page-title">Referrals</h1>
        <p className="udhr-page-subtitle">
          Incoming and outgoing referrals for {referralReport?.facilityName || 'your facility'}.
        </p>

        {!referralReport ? (
          <p className="udhr-empty-note">Loading...</p>
        ) : (
          <>
            <div className="udhr-stat-grid">
              <div className="udhr-stat-card">
                <p className="udhr-stat-label">Outgoing</p>
                <p className="udhr-stat-value">{referralReport.totalOutgoing}</p>
              </div>
              <div className="udhr-stat-card">
                <p className="udhr-stat-label">Incoming</p>
                <p className="udhr-stat-value">{referralReport.totalIncoming}</p>
              </div>
              <div className="udhr-stat-card">
                <p className="udhr-stat-label">Pending incoming</p>
                <p className="udhr-stat-value" style={{ color: referralReport.pendingIncoming > 0 ? 'var(--udhr-warning)' : undefined }}>
                  {referralReport.pendingIncoming}
                </p>
              </div>
              <div className="udhr-stat-card">
                <p className="udhr-stat-label">Emergency referrals</p>
                <p className="udhr-stat-value" style={{ color: referralReport.emergencyReferrals > 0 ? 'var(--udhr-danger)' : undefined }}>
                  {referralReport.emergencyReferrals}
                </p>
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px,1fr))', gap: '20px', marginBottom: '20px' }}>
              <div>
                <h2 className="udhr-section-title">Top destinations</h2>
                {referralReport.topDestinations.length === 0 ? (
                  <p className="udhr-empty-note">No outgoing referrals yet.</p>
                ) : (
                  referralReport.topDestinations.map((t, i) => (
                    <div key={i} className="udhr-row-card">
                      <p className="udhr-row-title">{t.facilityName}</p>
                      <span className="udhr-tag info">{t.referralCount}</span>
                    </div>
                  ))
                )}
              </div>
              <div>
                <h2 className="udhr-section-title">Top sources</h2>
                {referralReport.topSources.length === 0 ? (
                  <p className="udhr-empty-note">No incoming referrals yet.</p>
                ) : (
                  referralReport.topSources.map((t, i) => (
                    <div key={i} className="udhr-row-card">
                      <p className="udhr-row-title">{t.facilityName}</p>
                      <span className="udhr-tag info">{t.referralCount}</span>
                    </div>
                  ))
                )}
              </div>
            </div>

            <h2 className="udhr-section-title">Recent activity</h2>
            {referralReport.recentActivity.length === 0 ? (
              <p className="udhr-empty-note">No referral activity yet.</p>
            ) : (
              referralReport.recentActivity.map((r) => (
                <div key={r.id} className="udhr-card">
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '10px', marginBottom: '4px' }}>
                    <p className="udhr-row-title">{r.patient.firstName} {r.patient.lastName}</p>
                    <span className={`udhr-tag ${r.urgency === 'EMERGENCY' ? 'danger' : r.urgency === 'URGENT' ? 'warning' : 'info'}`}>{r.urgency}</span>
                  </div>
                  <p className="udhr-row-subtitle">{r.fromFacility.name} → {r.toFacility.name} · {r.reason}</p>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '6px' }}>
                    <span className="udhr-tag neutral">{r.status}</span>
                    <span className="udhr-row-subtitle">{new Date(r.referredAt).toLocaleDateString()}</span>
                  </div>
                </div>
              ))
            )}
          </>
        )}
      </>
    );

    const renderAnnouncementsPanel = () => (
      <>
        <h1 className="udhr-page-title">Public Announcements</h1>
        <p className="udhr-page-subtitle">
          Published ones rotate as a slideshow on the sign-in screen — use them for vaccination drives, clinic closures, outbreak notices.
        </p>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px,1fr))', gap: '20px' }}>
          <div className="udhr-record-card" style={{ alignSelf: 'flex-start' }}>
            <div className="udhr-record-body">
              <h3 style={{ fontSize: '14px', fontWeight: 700, margin: '0 0 14px' }}>Add announcement</h3>
              <form onSubmit={handleCreateAnnouncement}>
                <div className="udhr-form-group">
                  <label className="udhr-label">Title</label>
                  <input type="text" className="udhr-input" value={newAnnouncementForm.title} onChange={(e) => setNewAnnouncementForm({ ...newAnnouncementForm, title: e.target.value })} required />
                </div>
                <div className="udhr-form-group">
                  <label className="udhr-label">Message</label>
                  <textarea className="udhr-textarea" rows={3} value={newAnnouncementForm.message} onChange={(e) => setNewAnnouncementForm({ ...newAnnouncementForm, message: e.target.value })} required />
                </div>
                <div className="udhr-form-group">
                  <label className="udhr-label">Photo URL (optional)</label>
                  <input type="text" className="udhr-input" placeholder="https://..." value={newAnnouncementForm.photoUrl} onChange={(e) => setNewAnnouncementForm({ ...newAnnouncementForm, photoUrl: e.target.value })} />
                </div>
                <button type="submit" className="udhr-btn-primary">Publish announcement</button>
              </form>
            </div>
          </div>

          <div className="udhr-record-card" style={{ alignSelf: 'flex-start' }}>
            <div className="udhr-record-body">
              {announcements.length === 0 ? (
                <p className="udhr-empty-note">{loading ? 'Loading...' : 'No announcements yet.'}</p>
              ) : (
                announcements.map(a => (
                  <div key={a.id} className="udhr-list-row" style={{ flexDirection: 'column', alignItems: 'flex-start' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', width: '100%', alignItems: 'flex-start', gap: '10px' }}>
                      <div>
                        <p className="udhr-row-title">{a.title}</p>
                        <p className="udhr-row-subtitle">{a.message}</p>
                      </div>
                      <span className={`udhr-tag ${a.active ? 'info' : 'warning'}`} style={{ flexShrink: 0 }}>{a.active ? 'Published' : 'Unpublished'}</span>
                    </div>
                    <div style={{ display: 'flex', gap: '8px', marginTop: '10px' }}>
                      <button type="button" className="udhr-btn-neutral" onClick={() => handleToggleAnnouncement(a.id, !a.active)}>
                        {a.active ? 'Unpublish' : 'Publish'}
                      </button>
                      <button type="button" className="udhr-btn-soft-danger" onClick={() => handleDeleteAnnouncement(a.id)}>Delete</button>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      </>
    );

    return (
      <div className="udhr-shell">
        {isMobileNavOpen && <div className="udhr-shell-backdrop" onClick={() => setIsMobileNavOpen(false)} />}
        <aside className={`udhr-sidebar ${isMobileNavOpen ? 'open' : ''}`}>
          <div className="udhr-sidebar-header">
            <div className="udhr-page-logo">
              <div className="udhr-page-logo-chip"><Activity size={16} color="#fff" /></div>
              UDHR
            </div>
            <button type="button" className="udhr-sidebar-close" onClick={() => setIsMobileNavOpen(false)}><XCircle size={18} /></button>
          </div>
          <nav className="udhr-sidebar-nav">
            {adminNavItems.map(item => (
              <button key={item.key} type="button" className={`udhr-nav-item ${adminTab === item.key ? 'active' : ''}`} onClick={() => selectAdminNavTab(item.key)}>
                {item.icon} {item.label}
              </button>
            ))}
          </nav>
          <div className="udhr-sidebar-footer">
            <div className="udhr-user-row">
              <div className="udhr-user-avatar">{userName ? userName.charAt(0) : <User size={14} />}</div>
              <div style={{ minWidth: 0 }}>
                <p className="udhr-user-name">{userName}</p>
                <p className="udhr-user-role">{userRole}</p>
              </div>
            </div>
            <button type="button" className="udhr-logout-link" onClick={handleLogout}><LogOut size={15} /> Log out</button>
          </div>
        </aside>

        <main className="udhr-shell-main">
          <button type="button" className="udhr-mobile-menu-btn" onClick={() => setIsMobileNavOpen(true)}>
            <Menu size={16} /> Menu
          </button>

          {errorMessage && (
            <div className="udhr-alert-banner error">
              <AlertCircle size={16} />
              <span style={{ flex: 1 }}>{errorMessage}</span>
              <button type="button" onClick={() => setErrorMessage('')}>×</button>
            </div>
          )}
          {successMessage && (
            <div className="udhr-alert-banner success">
              <CheckCircle size={16} />
              <span style={{ flex: 1 }}>{successMessage}</span>
              <button type="button" onClick={() => setSuccessMessage('')}>×</button>
            </div>
          )}

          {adminTab === 'frontdesk' && renderFrontDeskPanel()}
          {adminTab === 'visitors' && renderVisitorsPanel()}
          {adminTab === 'staff' && renderStaffPanel()}
          {adminTab === 'facilities' && renderFacilitiesPanel()}
          {adminTab === 'referrals' && renderReferralsPanel()}
          {adminTab === 'announcements' && renderAnnouncementsPanel()}
        </main>
      </div>
    );
  }

  return (
    <div className="app-container">
      {/* Header */}
      <header className="app-header">
        <div className="logo-container">
          <Activity size={32} className="text-secondary" style={{ color: '#0ea5e9' }} />
          <span className="logo-text">Universal Digital Health Record</span>
        </div>
        {token && (
          <div className="flex items-center gap-4">
            <div className="text-right">
              <p style={{ color: '#fff', fontWeight: 600 }}>{userName}</p>
              <p className="text-muted" style={{ fontSize: '0.8rem' }}>
                {userRole === 'PATIENT' ? 'Patient Portal' : `Staff: ${userRole}`}
              </p>
            </div>
            <button className="btn btn-secondary" onClick={handleLogout} style={{ padding: '8px 16px' }}>
              <LogOut size={16} /> Logout
            </button>
          </div>
        )}
      </header>

      {/* Main Content Area */}
      <main style={{ minHeight: '60vh', display: 'flex', flexDirection: 'column', gap: '32px' }}>
        
        {/* Alerts */}
        {errorMessage && (
          <div className="glass-card" style={{ borderLeft: '4px solid var(--danger)', padding: '16px', background: 'rgba(239, 68, 68, 0.1)', display: 'flex', gap: '12px', alignItems: 'center' }}>
            <AlertCircle color="#ef4444" />
            <p style={{ color: '#ef4444', textAlign: 'left', flex: 1 }}>{errorMessage}</p>
            <button onClick={() => setErrorMessage('')} style={{ background: 'none', border: 'none', color: '#ef4444', cursor: 'pointer', fontWeight: 'bold' }}>X</button>
          </div>
        )}

        {successMessage && (
          <div className="glass-card" style={{ borderLeft: '4px solid var(--success)', padding: '16px', background: 'rgba(16, 185, 129, 0.1)', display: 'flex', gap: '12px', alignItems: 'center' }}>
            <CheckCircle color="#10b981" />
            <p style={{ color: '#10b981', textAlign: 'left', flex: 1 }}>{successMessage}</p>
            <button onClick={() => setSuccessMessage('')} style={{ background: 'none', border: 'none', color: '#10b981', cursor: 'pointer', fontWeight: 'bold' }}>X</button>
          </div>
        )}

        {/* Forced password change gate — blocks everything else until this is cleared */}
        {token && mustChangePassword && (
          <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', flex: 1, padding: '20px 0' }}>
            <div className="udhr-card" style={{ width: '420px', maxWidth: '100%', textAlign: 'center' }}>
              <Shield size={40} color="var(--udhr-primary)" style={{ margin: '0 auto 16px' }} />
              <h2 style={{ margin: '0 0 8px', fontSize: '19px', fontWeight: 700, color: 'var(--udhr-text)' }}>Set a new password</h2>
              <p style={{ margin: '0 0 24px', fontSize: '13.5px', color: 'var(--udhr-text-muted)' }}>
                This account was created with a temporary password. Choose your own before continuing.
              </p>
              <form onSubmit={handleChangePassword} style={{ textAlign: 'left' }}>
                <div className="udhr-form-group">
                  <label className="udhr-label" htmlFor="currentPassword">Temporary / Current Password</label>
                  <div className="udhr-password-field">
                    <input
                      type={showCurrentPassword ? 'text' : 'password'}
                      id="currentPassword"
                      className="udhr-input"
                      value={changePasswordForm.currentPassword}
                      onChange={(e) => setChangePasswordForm({ ...changePasswordForm, currentPassword: e.target.value })}
                      required
                    />
                    <button type="button" className="udhr-password-toggle" onClick={() => setShowCurrentPassword(!showCurrentPassword)} aria-label={showCurrentPassword ? 'Hide password' : 'Show password'}>
                      {showCurrentPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                    </button>
                  </div>
                </div>
                <div className="udhr-form-group">
                  <label className="udhr-label" htmlFor="newPassword">New Password</label>
                  <div className="udhr-password-field">
                    <input
                      type={showNewPassword ? 'text' : 'password'}
                      id="newPassword"
                      className="udhr-input"
                      placeholder="At least 8 characters"
                      value={changePasswordForm.newPassword}
                      onChange={(e) => setChangePasswordForm({ ...changePasswordForm, newPassword: e.target.value })}
                      required
                      minLength={8}
                    />
                    <button type="button" className="udhr-password-toggle" onClick={() => setShowNewPassword(!showNewPassword)} aria-label={showNewPassword ? 'Hide password' : 'Show password'}>
                      {showNewPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                    </button>
                  </div>
                </div>
                <div className="udhr-form-group">
                  <label className="udhr-label" htmlFor="confirmPassword">Confirm New Password</label>
                  <div className="udhr-password-field">
                    <input
                      type={showConfirmPassword ? 'text' : 'password'}
                      id="confirmPassword"
                      className="udhr-input"
                      value={changePasswordForm.confirmPassword}
                      onChange={(e) => setChangePasswordForm({ ...changePasswordForm, confirmPassword: e.target.value })}
                      required
                      minLength={8}
                    />
                    <button type="button" className="udhr-password-toggle" onClick={() => setShowConfirmPassword(!showConfirmPassword)} aria-label={showConfirmPassword ? 'Hide password' : 'Show password'}>
                      {showConfirmPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                    </button>
                  </div>
                </div>
                <button type="submit" className="udhr-btn-primary" style={{ width: '100%' }} disabled={loading}>
                  {loading ? 'Updating...' : 'Set Password & Continue'}
                </button>
              </form>
              <button
                type="button"
                className="udhr-btn-neutral"
                style={{ marginTop: '12px', width: '100%' }}
                onClick={handleLogout}
              >
                Cancel & Log Out
              </button>
            </div>
          </div>
        )}

      </main>

      {/* Footer */}
      <footer style={{ marginTop: 'auto', paddingTop: '40px', paddingBottom: '20px', borderTop: '1px solid rgba(255, 255, 255, 0.08)', textAlign: 'center' }}>
        <p className="text-muted" style={{ fontSize: '0.85rem' }}>
          &copy; {new Date().getFullYear()} Universal Digital Health Record System (UDHR). Authorized medical staff and patient access only.
        </p>
        <p className="text-muted" style={{ fontSize: '0.75rem', marginTop: '4px' }}>
          System complies with the National Health Act and POPI Act of South Africa. Portals powered by Infermedica Triage & OpenFDA Databases.
        </p>
      </footer>
    </div>
  );
}

export default App;
