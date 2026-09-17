import React, { useState, useEffect } from 'react';
import {
  Activity, Heart, AlertTriangle, Shield, ShieldAlert, User, LogOut, Search, PlusCircle,
  Calendar, MapPin, Phone, CheckCircle, XCircle, FileText, Pill, Compass, Clock,
  Clipboard, RefreshCw, AlertCircle, FileSpreadsheet, Upload, Barcode, Building2, X,
  Download, BellRing
} from 'lucide-react';
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';
import { Button } from '@/components/ui/button';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Select, SelectTrigger, SelectValue, SelectContent, SelectItem } from '@/components/ui/select';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { cn } from '@/lib/utils';

function App() {
  // Authentication State
  const [token, setToken] = useState(localStorage.getItem('token') || '');
  const [userRole, setUserRole] = useState(localStorage.getItem('role') || '');
  const [userIdNumber, setUserIdNumber] = useState(localStorage.getItem('idNumber') || '');
  const [userName, setUserName] = useState(localStorage.getItem('userName') || '');
  const [userFacilityId, setUserFacilityId] = useState(localStorage.getItem('facilityId') || '');
  const [mustChangePassword, setMustChangePassword] = useState(localStorage.getItem('mustChangePassword') === 'true');
  const [changePasswordForm, setChangePasswordForm] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });
  
  const [loginRole, setLoginRole] = useState('patient'); // 'patient' or 'staff'
  const [staffNumber, setStaffNumber] = useState('DOC001');
  const [staffPassword, setStaffPassword] = useState('Doctor@123');
  const [patientIdNumber, setPatientIdNumber] = useState('9001015000083');
  const [patientDob, setPatientDob] = useState('1990-01-01');
  
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
  const [activeTabStaff, setActiveTabStaff] = useState('patients'); // 'patients' or 'alerts'

  // Staff Dashboard Data State
  const [searchId, setSearchId] = useState('9001015000083');
  const [searchedPatientRecord, setSearchedPatientRecord] = useState(null);
  
  // Forms for Staff
  const [patientRegForm, setPatientRegForm] = useState({
    idNumber: '', firstName: '', lastName: '', dateOfBirth: '', gender: 'MALE', contactNumber: '', address: '', email: '',
    nextOfKinFirstName: '', nextOfKinLastName: '', nextOfKinRelationship: '', nextOfKinPhone: '',
    motherIdNumber: '', birthWeightGrams: '', birthLengthCm: '', apgarScore1Min: '', apgarScore5Min: ''
  });
  const [isNewbornMode, setIsNewbornMode] = useState(false);
  const [addDiagnosisForm, setAddDiagnosisForm] = useState({
    patientId: '', conditionName: '', notes: ''
  });
  const [addPrescriptionForm, setAddPrescriptionForm] = useState({
    patientId: '', medicationName: '', dosage: '', frequency: 'Once daily', durationDays: 7, notes: ''
  });
  const [addAlertForm, setAddAlertForm] = useState({
    patientId: '', severity: 'HIGH', message: ''
  });
  const [addLabResultForm, setAddLabResultForm] = useState({
    patientId: '', testName: '', result: '', unit: '', normalRange: '', notes: ''
  });
  const [showCatchUpForm, setShowCatchUpForm] = useState(false);
  const [catchUpForm, setCatchUpForm] = useState({
    vaccineName: '', doseNumber: '', scheduledDate: '', administeredDate: '', notes: ''
  });

  // Admin: Staff Management state
  const [staffList, setStaffList] = useState([]);
  const [facilitiesList, setFacilitiesList] = useState([]);
  const [staffRegForm, setStaffRegForm] = useState({
    staffNumber: '', firstName: '', lastName: '', role: 'NURSE', facilityId: '', email: '', contactNumber: '', password: ''
  });
  const [facilityRegForm, setFacilityRegForm] = useState({
    name: '', type: 'CLINIC', province: 'Mpumalanga', address: ''
  });

  // Reception: Queue & Vitals state
  const [todayQueue, setTodayQueue] = useState([]);
  const [showCheckInForm, setShowCheckInForm] = useState(false);
  const [checkInForm, setCheckInForm] = useState({ department: 'GP', reason: '', urgency: 'GREEN' });
  const [vitalsFormFor, setVitalsFormFor] = useState(null); // queue entry id currently showing the vitals form
  const [vitalsForm, setVitalsForm] = useState({
    systolicBp: '', diastolicBp: '', temperatureC: '', pulseBpm: '', respiratoryRate: '',
    oxygenSaturation: '', weightKg: '', heightCm: '', glucoseMmol: '', notes: ''
  });

  // Discharge & Referral state
  const [showDischargeForm, setShowDischargeForm] = useState(false);
  const [dischargeForm, setDischargeForm] = useState({ dischargeOutcome: 'HOME', dischargeSummary: '', followUpDate: '' });
  const [showReferForm, setShowReferForm] = useState(false);
  const [referForm, setReferForm] = useState({ toFacilityId: '', urgency: 'ROUTINE', reason: '', clinicalSummary: '' });
  const [incomingReferrals, setIncomingReferrals] = useState([]);
  const [outgoingReferrals, setOutgoingReferrals] = useState([]);

  // Pharmacy Dispensing state
  const [dispenseFormFor, setDispenseFormFor] = useState(null); // prescription id currently showing the dispense form
  const [dispenseForm, setDispenseForm] = useState({ quantityDispensed: '', daysSupply: '', pharmacyNotes: '' });

  // Stock/Inventory state
  const [stockList, setStockList] = useState([]);
  const [stockItemForm, setStockItemForm] = useState({ medicationName: '', unit: 'tablets', quantityOnHand: '', reorderLevel: '' });
  const [receiveFormFor, setReceiveFormFor] = useState(null); // stock item id currently showing the receive form
  const [receiveForm, setReceiveForm] = useState({ type: 'RECEIVE', quantityChange: '', notes: '' });
  const [stockHistoryFor, setStockHistoryFor] = useState(null); // stock item id currently showing its transaction history
  const [stockHistory, setStockHistory] = useState([]);
  const [stockReport, setStockReport] = useState(null); // admin-only pharmacy stock report
  const [dispenseReport, setDispenseReport] = useState(null); // admin-only pharmacy dispensing report
  const [referralReport, setReferralReport] = useState(null); // admin-only facility-wide referral report
  const [prescriptionReport, setPrescriptionReport] = useState(null); // admin-only facility-wide prescription report
  const [labResultReport, setLabResultReport] = useState(null); // admin-only facility-wide lab result report
  const [reportDateRange, setReportDateRange] = useState({ startDate: '', endDate: '' }); // shared date filter for all admin reports
  const [lowStockAlert, setLowStockAlert] = useState(null); // { items } shown once after login, dismissible

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
      localStorage.setItem('facilityId', data.facilityId || '');
      localStorage.setItem('mustChangePassword', data.mustChangePassword ? 'true' : 'false');

      setToken(data.token);
      setUserRole(data.role);
      setUserIdNumber(data.idNumber || data.staffNumber);
      setUserName(data.fullName);
      setUserFacilityId(data.facilityId || '');
      setMustChangePassword(!!data.mustChangePassword);
      setSuccessMessage(data.mustChangePassword ? 'Logged in — please set a new password to continue.' : 'Logged in successfully!');
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
    localStorage.removeItem('facilityId');
    localStorage.removeItem('mustChangePassword');
    setToken('');
    setUserRole('');
    setUserIdNumber('');
    setUserName('');
    setUserFacilityId('');
    setMustChangePassword(false);
    setChangePasswordForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
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
  };

  // Forced first-login password change (new/admin-created staff accounts)
  const handleChangePassword = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');

    if (changePasswordForm.newPassword !== changePasswordForm.confirmPassword) {
      setErrorMessage('New password and confirmation do not match.');
      return;
    }
    if (changePasswordForm.newPassword.length < 8) {
      setErrorMessage('New password must be at least 8 characters.');
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
      const data = await response.json().catch(() => null);
      if (!response.ok) {
        throw new Error(typeof data === 'string' ? data : data?.message || 'Failed to update password');
      }

      // The old token's mustChangePassword claim is baked in and permanent —
      // swap in the freshly issued token or every subsequent request keeps
      // getting rejected by the filter even though the password did change.
      localStorage.setItem('token', data.token);
      localStorage.setItem('mustChangePassword', 'false');
      setToken(data.token);
      setMustChangePassword(false);
      setSuccessMessage('Password updated. Welcome to UDHR.');
      setChangePasswordForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
    } catch (err) {
      setErrorMessage(err.message);
    } finally {
      setLoading(false);
    }
  };

  // Fetch data on login (skip while a forced password change is pending —
  // the API would reject these calls anyway until it's done)
  useEffect(() => {
    if (token && !mustChangePassword) {
      if (userRole === 'PATIENT') {
        fetchPatientPortalData();
      } else {
        fetchClinicalAlerts();
        fetchFacilitiesList(); // needed for the "Refer to Another Facility" destination picker
        checkLowStockOnLogin();
      }
    }
  }, [token, userRole, mustChangePassword]);

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

  // Admin: Staff Management
  const fetchStaffList = async () => {
    try {
      const response = await fetch('/api/staff', { headers: getAuthHeaders() });
      if (response.ok) {
        setStaffList(await response.json());
      }
    } catch (err) {
      console.error("Error fetching staff list", err);
    }
  };

  const fetchFacilitiesList = async () => {
    try {
      const response = await fetch('/api/facilities', { headers: getAuthHeaders() });
      if (response.ok) {
        const data = await response.json();
        setFacilitiesList(data);
        setStaffRegForm(prev => (prev.facilityId ? prev : { ...prev, facilityId: data.length > 0 ? String(data[0].id) : '' }));
      }
    } catch (err) {
      console.error("Error fetching facilities list", err);
    }
  };

  const handleRegisterStaff = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');
    setLoading(true);
    try {
      const payload = {
        ...staffRegForm,
        facilityId: staffRegForm.facilityId ? Number(staffRegForm.facilityId) : undefined
      };
      const response = await fetch('/api/staff', {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(payload)
      });
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.message || 'Failed to register staff member');
      }
      setSuccessMessage(`Staff member '${data.firstName} ${data.lastName}' (${data.staffNumber}) registered successfully!`);
      setStaffRegForm({
        staffNumber: '', firstName: '', lastName: '', role: 'NURSE',
        facilityId: facilitiesList.length > 0 ? String(facilitiesList[0].id) : '', email: '', contactNumber: '', password: ''
      });
      fetchStaffList();
    } catch (err) {
      setErrorMessage(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleDeactivateStaff = async (staffId) => {
    setErrorMessage('');
    setSuccessMessage('');
    setLoading(true);
    try {
      const response = await fetch(`/api/staff/${staffId}/deactivate`, {
        method: 'PUT',
        headers: getAuthHeaders()
      });
      if (!response.ok) throw new Error('Failed to deactivate staff member');
      setSuccessMessage('Staff member deactivated.');
      fetchStaffList();
    } catch (err) {
      setErrorMessage(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleRegisterFacility = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');
    setLoading(true);
    try {
      const response = await fetch('/api/facilities', {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(facilityRegForm)
      });
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.message || 'Failed to register facility');
      }
      setSuccessMessage(`Facility '${data.name}' registered successfully!`);
      setFacilityRegForm({ name: '', type: 'CLINIC', province: 'Mpumalanga', address: '' });
      fetchFacilitiesList();
    } catch (err) {
      setErrorMessage(err.message);
    } finally {
      setLoading(false);
    }
  };

  // Reception: Queue
  const fetchTodayQueue = async () => {
    try {
      const response = await fetch('/api/queue/today', { headers: getAuthHeaders() });
      if (response.ok) {
        setTodayQueue(await response.json());
      }
    } catch (err) {
      console.error("Error fetching today's queue", err);
    }
  };

  const handleCheckIn = async (e, patientId) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');
    setLoading(true);
    try {
      const response = await fetch('/api/queue/check-in', {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({ patientId, ...checkInForm })
      });
      const data = await response.json();
      if (!response.ok) throw new Error(typeof data === 'string' ? data : data.message || 'Failed to check in patient');
      setSuccessMessage(`Checked in — queue number ${data.queueNumber} (${data.department}).`);
      setShowCheckInForm(false);
      setCheckInForm({ department: 'GP', reason: '', urgency: 'GREEN' });
      fetchTodayQueue();
    } catch (err) {
      setErrorMessage(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateQueueUrgency = async (id, urgency) => {
    try {
      const response = await fetch(`/api/queue/${id}/urgency`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({ urgency })
      });
      if (!response.ok) throw new Error('Failed to update urgency');
      fetchTodayQueue();
    } catch (err) {
      setErrorMessage(err.message);
    }
  };

  const handleCallIntoConsultation = async (id) => {
    setErrorMessage('');
    setSuccessMessage('');
    try {
      const response = await fetch(`/api/queue/${id}/call`, { method: 'POST', headers: getAuthHeaders() });
      if (!response.ok) throw new Error('Failed to call patient in');
      setSuccessMessage('Patient called into consultation.');
      fetchTodayQueue();
    } catch (err) {
      setErrorMessage(err.message);
    }
  };

  const handleCompleteQueueEntry = async (id) => {
    setErrorMessage('');
    setSuccessMessage('');
    try {
      const response = await fetch(`/api/queue/${id}/complete`, { method: 'POST', headers: getAuthHeaders() });
      if (!response.ok) throw new Error('Failed to complete queue entry');
      setSuccessMessage('Marked as completed.');
      fetchTodayQueue();
    } catch (err) {
      setErrorMessage(err.message);
    }
  };

  const handleCancelQueueEntry = async (id) => {
    setErrorMessage('');
    setSuccessMessage('');
    try {
      const response = await fetch(`/api/queue/${id}/cancel`, { method: 'POST', headers: getAuthHeaders() });
      if (!response.ok) throw new Error('Failed to cancel queue entry');
      setSuccessMessage('Queue entry cancelled.');
      fetchTodayQueue();
    } catch (err) {
      setErrorMessage(err.message);
    }
  };

  // Reception: Vitals
  const handleRecordVitals = async (e, patientId, queueEntryId) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');
    setLoading(true);
    try {
      const payload = {
        patientId,
        queueEntryId,
        systolicBp: vitalsForm.systolicBp ? parseInt(vitalsForm.systolicBp, 10) : undefined,
        diastolicBp: vitalsForm.diastolicBp ? parseInt(vitalsForm.diastolicBp, 10) : undefined,
        temperatureC: vitalsForm.temperatureC ? parseFloat(vitalsForm.temperatureC) : undefined,
        pulseBpm: vitalsForm.pulseBpm ? parseInt(vitalsForm.pulseBpm, 10) : undefined,
        respiratoryRate: vitalsForm.respiratoryRate ? parseInt(vitalsForm.respiratoryRate, 10) : undefined,
        oxygenSaturation: vitalsForm.oxygenSaturation ? parseFloat(vitalsForm.oxygenSaturation) : undefined,
        weightKg: vitalsForm.weightKg ? parseFloat(vitalsForm.weightKg) : undefined,
        heightCm: vitalsForm.heightCm ? parseFloat(vitalsForm.heightCm) : undefined,
        glucoseMmol: vitalsForm.glucoseMmol ? parseFloat(vitalsForm.glucoseMmol) : undefined,
        notes: vitalsForm.notes || undefined
      };
      const response = await fetch('/api/vitals', {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(payload)
      });
      const data = await response.json();
      if (!response.ok) throw new Error(typeof data === 'string' ? data : data.message || 'Failed to record vitals');
      setSuccessMessage('Vitals recorded.');
      setVitalsFormFor(null);
      setVitalsForm({
        systolicBp: '', diastolicBp: '', temperatureC: '', pulseBpm: '', respiratoryRate: '',
        oxygenSaturation: '', weightKg: '', heightCm: '', glucoseMmol: '', notes: ''
      });
      fetchTodayQueue();
      if (searchedPatientRecord && searchedPatientRecord.patient.id === patientId) {
        handleSearchPatient();
      }
    } catch (err) {
      setErrorMessage(err.message);
    } finally {
      setLoading(false);
    }
  };

  // Discharge a patient's active visit
  const handleDischarge = async (e, patientId) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');
    setLoading(true);
    try {
      const response = await fetch('/api/visits/discharge', {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({
          patientId,
          dischargeOutcome: dischargeForm.dischargeOutcome,
          dischargeSummary: dischargeForm.dischargeSummary || undefined,
          followUpDate: dischargeForm.followUpDate || undefined
        })
      });
      const data = await response.json();
      if (!response.ok) throw new Error(typeof data === 'string' ? data : data.message || 'Failed to discharge patient');
      setSuccessMessage('Patient discharged.');
      setShowDischargeForm(false);
      setDischargeForm({ dischargeOutcome: 'HOME', dischargeSummary: '', followUpDate: '' });
      handleSearchPatient();
    } catch (err) {
      setErrorMessage(err.message);
    } finally {
      setLoading(false);
    }
  };

  // Refer a patient to another facility
  const handleRefer = async (e, patientId) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');
    setLoading(true);
    try {
      const response = await fetch('/api/referrals', {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({
          patientId,
          toFacilityId: Number(referForm.toFacilityId),
          urgency: referForm.urgency,
          reason: referForm.reason,
          clinicalSummary: referForm.clinicalSummary || undefined
        })
      });
      const data = await response.json();
      if (!response.ok) throw new Error(typeof data === 'string' ? data : data.message || 'Failed to create referral');
      setSuccessMessage(`Patient referred to ${facilitiesList.find(f => f.id === Number(referForm.toFacilityId))?.name || 'destination facility'}.`);
      setShowReferForm(false);
      setReferForm({ toFacilityId: '', urgency: 'ROUTINE', reason: '', clinicalSummary: '' });
      handleSearchPatient();
    } catch (err) {
      setErrorMessage(err.message);
    } finally {
      setLoading(false);
    }
  };

  // Referrals tab: incoming (to my facility) and outgoing (from my facility)
  const fetchIncomingReferrals = async () => {
    try {
      const response = await fetch('/api/referrals/incoming', { headers: getAuthHeaders() });
      if (response.ok) setIncomingReferrals(await response.json());
    } catch (err) {
      console.error('Error fetching incoming referrals', err);
    }
  };

  const fetchOutgoingReferrals = async () => {
    try {
      const response = await fetch('/api/referrals/outgoing', { headers: getAuthHeaders() });
      if (response.ok) setOutgoingReferrals(await response.json());
    } catch (err) {
      console.error('Error fetching outgoing referrals', err);
    }
  };

  const handleRespondToReferral = async (id, status) => {
    setErrorMessage('');
    setSuccessMessage('');
    try {
      const response = await fetch(`/api/referrals/${id}/respond`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({ status })
      });
      if (!response.ok) throw new Error('Failed to update referral');
      setSuccessMessage(`Referral marked as ${status.toLowerCase()}.`);
      fetchIncomingReferrals();
      fetchOutgoingReferrals();
    } catch (err) {
      setErrorMessage(err.message);
    }
  };

  // Pharmacy: dispense a prescription
  const handleDispense = async (e, prescriptionId) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');
    setLoading(true);
    try {
      const payload = {
        prescriptionId,
        quantityDispensed: dispenseForm.quantityDispensed,
        daysSupply: dispenseForm.daysSupply ? Number(dispenseForm.daysSupply) : undefined,
        pharmacyNotes: dispenseForm.pharmacyNotes
      };
      const response = await fetch('/api/dispensing', {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(payload)
      });
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.message || data || 'Failed to record dispensing');
      }
      setSuccessMessage(`${data.quantityDispensed} dispensed successfully.`);
      setDispenseForm({ quantityDispensed: '', daysSupply: '', pharmacyNotes: '' });
      setDispenseFormFor(null);
      handleSearchPatient(); // Refresh record
    } catch (err) {
      setErrorMessage(err.message);
    } finally {
      setLoading(false);
    }
  };

  // Stock / Inventory
  const fetchStockList = async () => {
    try {
      const response = await fetch('/api/stock', { headers: getAuthHeaders() });
      if (response.ok) setStockList(await response.json());
    } catch (err) {
      console.error('Error fetching stock list', err);
    }
  };

  // Checked once right after login so staff see it immediately, rather than
  // only discovering low stock if they happen to open the Stock tab.
  const checkLowStockOnLogin = async () => {
    try {
      const response = await fetch('/api/stock', { headers: getAuthHeaders() });
      if (response.ok) {
        const items = await response.json();
        const low = items.filter(s => s.quantityOnHand <= s.reorderLevel);
        setLowStockAlert(low.length > 0 ? { items: low } : null);
      }
    } catch (err) {
      console.error('Error checking low stock on login', err);
    }
  };

  const fetchStockReport = async (qs = '') => {
    try {
      const response = await fetch(`/api/stock/report${qs}`, { headers: getAuthHeaders() });
      if (response.ok) setStockReport(await response.json());
    } catch (err) {
      console.error('Error fetching stock report', err);
    }
  };

  const fetchDispenseReport = async (qs = '') => {
    try {
      const response = await fetch(`/api/dispensing/report${qs}`, { headers: getAuthHeaders() });
      if (response.ok) setDispenseReport(await response.json());
    } catch (err) {
      console.error('Error fetching dispensing report', err);
    }
  };

  const fetchReferralReport = async (qs = '') => {
    try {
      const response = await fetch(`/api/referrals/report${qs}`, { headers: getAuthHeaders() });
      if (response.ok) setReferralReport(await response.json());
    } catch (err) {
      console.error('Error fetching referral report', err);
    }
  };

  const fetchPrescriptionReport = async (qs = '') => {
    try {
      const response = await fetch(`/api/prescriptions/report${qs}`, { headers: getAuthHeaders() });
      if (response.ok) setPrescriptionReport(await response.json());
    } catch (err) {
      console.error('Error fetching prescription report', err);
    }
  };

  const fetchLabResultReport = async (qs = '') => {
    try {
      const response = await fetch(`/api/lab-results/report${qs}`, { headers: getAuthHeaders() });
      if (response.ok) setLabResultReport(await response.json());
    } catch (err) {
      console.error('Error fetching lab result report', err);
    }
  };

  // Shared by the admin reports' date-range filter bar: refetches all five
  // reports with the same [startDate, endDate] window (either may be blank
  // for an open-ended bound, both blank for all-time).
  const fetchAllReports = (startDate, endDate) => {
    const params = new URLSearchParams();
    if (startDate) params.set('startDate', startDate);
    if (endDate) params.set('endDate', endDate);
    const qs = params.toString() ? `?${params.toString()}` : '';
    fetchStockReport(qs);
    fetchDispenseReport(qs);
    fetchReferralReport(qs);
    fetchPrescriptionReport(qs);
    fetchLabResultReport(qs);
  };

  // Downloads a report's CSV export honoring the currently-applied date
  // range. A plain <a href> can't carry the Authorization header these
  // endpoints require, so this fetches the file as a blob and triggers the
  // save via a temporary object URL instead.
  const downloadReportCsv = async (path, filename) => {
    setErrorMessage('');
    try {
      const params = new URLSearchParams();
      if (reportDateRange.startDate) params.set('startDate', reportDateRange.startDate);
      if (reportDateRange.endDate) params.set('endDate', reportDateRange.endDate);
      const qs = params.toString() ? `?${params.toString()}` : '';
      const response = await fetch(`${path}${qs}`, { headers: getAuthHeaders() });
      if (!response.ok) {
        const data = await response.json().catch(() => null);
        throw new Error((data && data.message) || 'Failed to export CSV');
      }
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setErrorMessage(err.message);
    }
  };

  const reportDateRangeLabel = () => {
    const { startDate, endDate } = reportDateRange;
    if (!startDate && !endDate) return 'All Time';
    if (startDate && endDate) return `${startDate} to ${endDate}`;
    return startDate ? `From ${startDate}` : `Through ${endDate}`;
  };

  // Short suffix for individual stat labels (e.g. "Units Received (...)").
  // Stays "All-Time" when unfiltered; once a range is applied the exact
  // dates are already shown in the filter bar (and the PDF header), so this
  // just flags that the number is scoped rather than repeating them in
  // every tile.
  const reportRangeSuffix = () => {
    const { startDate, endDate } = reportDateRange;
    return (!startDate && !endDate) ? 'All-Time' : 'Selected Range';
  };

  // Shared header (title/facility/range/generated-at) every report PDF
  // starts with, so each export function only has to lay out its own body.
  const startPdfDoc = (title, facilityName) => {
    const doc = new jsPDF();
    doc.setFontSize(16);
    doc.text(title, 14, 18);
    doc.setFontSize(10);
    doc.setTextColor(100);
    doc.text(facilityName || '', 14, 25);
    doc.text(`Range: ${reportDateRangeLabel()}`, 14, 30);
    doc.text(`Generated: ${new Date().toLocaleString()}`, 14, 35);
    doc.setTextColor(0);
    return doc;
  };

  const addSectionTable = (doc, y, heading, head, body) => {
    if (heading) {
      doc.setFontSize(11);
      doc.setTextColor(0);
      doc.text(heading, 14, y);
      y += 4;
    }
    autoTable(doc, {
      startY: y,
      head: [head],
      body,
      styles: { fontSize: 8 },
      headStyles: { fillColor: [79, 70, 229] },
      margin: { left: 14, right: 14 },
    });
    return doc.lastAutoTable.finalY + 10;
  };

  const exportStockPdf = () => {
    if (!stockReport) return;
    const doc = startPdfDoc('Pharmacy Stock Report', stockReport.facilityName);
    let y = addSectionTable(doc, 42, 'Summary', ['Metric', 'Value'], [
      ['Medications Tracked', String(stockReport.totalMedicationsTracked)],
      ['Low Stock Items', String(stockReport.lowStockCount)],
      [`Units Received (${reportRangeSuffix()})`, String(stockReport.totalUnitsReceived)],
      [`Units Dispensed (${reportRangeSuffix()})`, String(stockReport.totalUnitsDispensed)],
      [`Units Written Off (${reportRangeSuffix()})`, String(stockReport.totalUnitsWrittenOff)],
    ]);
    if (stockReport.lowStockItems.length > 0) {
      y = addSectionTable(doc, y, 'Needs Reordering', ['Medication', 'On Hand', 'Reorder Level', 'Unit'],
        stockReport.lowStockItems.map(i => [i.medicationName, String(i.quantityOnHand), String(i.reorderLevel), i.unit]));
    }
    addSectionTable(doc, y, 'Recent Stock Activity', ['Date', 'Type', 'Medication', 'Qty Change', 'Staff', 'Notes'],
      stockReport.recentTransactions.map(t => [
        new Date(t.createdAt).toLocaleString(), t.type, t.stockItem?.medicationName || '',
        `${t.quantityChange > 0 ? '+' : ''}${t.quantityChange} ${t.stockItem?.unit || ''}`,
        `${t.staff?.firstName || ''} ${t.staff?.lastName || ''}`, t.notes || ''
      ]));
    doc.save('stock-report.pdf');
  };

  const exportDispensePdf = () => {
    if (!dispenseReport) return;
    const doc = startPdfDoc('Pharmacy Dispensing Report', dispenseReport.facilityName);
    let y = addSectionTable(doc, 42, 'Summary', ['Metric', 'Value'], [
      ['Total Dispense Events', String(dispenseReport.totalDispenseEvents)],
      ['Dispensed Today', String(dispenseReport.dispensedToday)],
      ['Unique Patients Served', String(dispenseReport.uniquePatientsServed)],
    ]);
    if (dispenseReport.topMedications.length > 0) {
      y = addSectionTable(doc, y, 'Top Dispensed Medications', ['Medication', 'Dispenses', 'Units'],
        dispenseReport.topMedications.map(m => [m.medicationName, String(m.dispenseCount), String(m.totalUnitsDispensed)]));
    }
    addSectionTable(doc, y, 'Recent Dispensing Activity', ['Date', 'Medication', 'Quantity', 'Patient', 'Dispensed By'],
      dispenseReport.recentDispenses.map(d => [
        new Date(d.dispensedAt).toLocaleString(), d.prescription?.medication || '', d.quantityDispensed || '',
        `${d.patient?.firstName || ''} ${d.patient?.lastName || ''}`,
        `${d.dispensedBy?.firstName || ''} ${d.dispensedBy?.lastName || ''}`
      ]));
    doc.save('dispensing-report.pdf');
  };

  const exportReferralPdf = () => {
    if (!referralReport) return;
    const doc = startPdfDoc('Facility Referral Report', referralReport.facilityName);
    let y = addSectionTable(doc, 42, 'Summary', ['Metric', 'Value'], [
      [`Outgoing Referrals (${reportRangeSuffix()})`, String(referralReport.totalOutgoing)],
      [`Incoming Referrals (${reportRangeSuffix()})`, String(referralReport.totalIncoming)],
      ['Pending Incoming (Needs Response)', String(referralReport.pendingIncoming)],
      [`Emergency Referrals (${reportRangeSuffix()})`, String(referralReport.emergencyReferrals)],
    ]);
    if (referralReport.topDestinations.length > 0) {
      y = addSectionTable(doc, y, 'Top Destination Facilities', ['Facility', 'Referrals'],
        referralReport.topDestinations.map(f => [f.facilityName, String(f.referralCount)]));
    }
    if (referralReport.topSources.length > 0) {
      y = addSectionTable(doc, y, 'Top Source Facilities', ['Facility', 'Referrals'],
        referralReport.topSources.map(f => [f.facilityName, String(f.referralCount)]));
    }
    addSectionTable(doc, y, 'Recent Referral Activity', ['Date', 'Direction', 'Patient', 'Facility', 'Urgency', 'Status', 'Reason'],
      referralReport.recentActivity.map(r => {
        const isOutgoing = r.fromFacility?.name === referralReport.facilityName;
        return [
          new Date(r.referredAt).toLocaleString(), isOutgoing ? 'Sent' : 'Received',
          `${r.patient?.firstName || ''} ${r.patient?.lastName || ''}`,
          isOutgoing ? r.toFacility?.name : r.fromFacility?.name,
          r.urgency, r.status, r.reason || ''
        ];
      }));
    doc.save('referral-report.pdf');
  };

  const exportPrescriptionPdf = () => {
    if (!prescriptionReport) return;
    const doc = startPdfDoc('Facility Prescription Report', prescriptionReport.facilityName);
    let y = addSectionTable(doc, 42, 'Summary', ['Metric', 'Value'], [
      [`Total Prescriptions (${reportRangeSuffix()})`, String(prescriptionReport.totalPrescriptions)],
      ['Active Prescriptions', String(prescriptionReport.activePrescriptions)],
      ['Issued Today', String(prescriptionReport.issuedToday)],
      ['Unique Patients Prescribed', String(prescriptionReport.uniquePatientsPrescribed)],
    ]);
    if (prescriptionReport.topMedications.length > 0) {
      y = addSectionTable(doc, y, 'Top Prescribed Medications', ['Medication', 'Count'],
        prescriptionReport.topMedications.map(m => [m.medicationName, String(m.prescriptionCount)]));
    }
    if (prescriptionReport.topPrescribers.length > 0) {
      y = addSectionTable(doc, y, 'Top Prescribers', ['Prescriber', 'Count'],
        prescriptionReport.topPrescribers.map(p => [`Dr. ${p.prescriberName}`, String(p.prescriptionCount)]));
    }
    addSectionTable(doc, y, 'Recent Prescriptions', ['Date', 'Medication', 'Dosage', 'Patient', 'Doctor', 'Active'],
      prescriptionReport.recentPrescriptions.map(p => [
        new Date(p.createdAt).toLocaleString(), p.medication, p.dosage || '',
        `${p.patient?.firstName || ''} ${p.patient?.lastName || ''}`,
        `Dr. ${p.doctor?.firstName || ''} ${p.doctor?.lastName || ''}`, p.active ? 'Yes' : 'No'
      ]));
    doc.save('prescription-report.pdf');
  };

  const exportLabResultPdf = () => {
    if (!labResultReport) return;
    const doc = startPdfDoc('Facility Lab Results Report', labResultReport.facilityName);
    let y = addSectionTable(doc, 42, 'Summary', ['Metric', 'Value'], [
      [`Total Lab Results (${reportRangeSuffix()})`, String(labResultReport.totalLabResults)],
      ['Results Today', String(labResultReport.resultsToday)],
      ['Unique Patients Tested', String(labResultReport.uniquePatientsTested)],
      ['Unique Test Types', String(labResultReport.uniqueTestTypes)],
    ]);
    if (labResultReport.topTestTypes.length > 0) {
      y = addSectionTable(doc, y, 'Top Test Types', ['Test', 'Count'],
        labResultReport.topTestTypes.map(t => [t.testName, String(t.testCount)]));
    }
    if (labResultReport.topOrderingStaff.length > 0) {
      y = addSectionTable(doc, y, 'Top Ordering Staff', ['Staff', 'Count'],
        labResultReport.topOrderingStaff.map(s => [s.staffName, String(s.testCount)]));
    }
    addSectionTable(doc, y, 'Recent Lab Results', ['Date', 'Test', 'Result', 'Normal Range', 'Patient', 'Staff'],
      labResultReport.recentResults.map(r => [
        new Date(r.testDate).toLocaleString(), r.testName, `${r.result || ''} ${r.unit || ''}`.trim(),
        r.normalRange || '', `${r.patient?.firstName || ''} ${r.patient?.lastName || ''}`,
        `${r.staff?.firstName || ''} ${r.staff?.lastName || ''}`
      ]));
    doc.save('lab-results-report.pdf');
  };

  const handleAddStockItem = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');
    setLoading(true);
    try {
      const payload = {
        ...stockItemForm,
        quantityOnHand: stockItemForm.quantityOnHand ? Number(stockItemForm.quantityOnHand) : 0,
        reorderLevel: stockItemForm.reorderLevel ? Number(stockItemForm.reorderLevel) : 0
      };
      const response = await fetch('/api/stock', {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(payload)
      });
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.message || data || 'Failed to add stock item');
      }
      setSuccessMessage(`'${data.medicationName}' is now tracked in facility inventory.`);
      setStockItemForm({ medicationName: '', unit: 'tablets', quantityOnHand: '', reorderLevel: '' });
      fetchStockList();
    } catch (err) {
      setErrorMessage(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleStockAdjustment = async (e, stockItemId) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');
    setLoading(true);
    try {
      const isReceive = receiveForm.type === 'RECEIVE';
      const magnitude = Math.abs(Number(receiveForm.quantityChange) || 0);
      const payload = {
        stockItemId,
        quantityChange: isReceive ? magnitude : -magnitude,
        notes: receiveForm.notes
      };
      const response = await fetch(`/api/stock/${isReceive ? 'receive' : 'adjust'}`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(payload)
      });
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.message || data || 'Failed to update stock');
      }
      setSuccessMessage(isReceive ? `Stock received: ${data.medicationName} now at ${data.quantityOnHand} ${data.unit}.` : `Stock written off: ${data.medicationName} now at ${data.quantityOnHand} ${data.unit}.`);
      setReceiveForm({ type: 'RECEIVE', quantityChange: '', notes: '' });
      setReceiveFormFor(null);
      fetchStockList();
    } catch (err) {
      setErrorMessage(err.message);
    } finally {
      setLoading(false);
    }
  };

  const toggleStockHistory = async (stockItemId) => {
    if (stockHistoryFor === stockItemId) {
      setStockHistoryFor(null);
      return;
    }
    try {
      const response = await fetch(`/api/stock/${stockItemId}/history`, { headers: getAuthHeaders() });
      if (response.ok) {
        setStockHistory(await response.json());
        setStockHistoryFor(stockItemId);
      }
    } catch (err) {
      console.error('Error fetching stock history', err);
    }
  };

  // Staff search patient
  const handleSearchPatient = async (e) => {
    if (e) e.preventDefault();
    setErrorMessage('');
    setLoading(true);
    setSearchedPatientRecord(null);
    setPatientAdherence(null);
    setPatientTimeline(null);

    try {
      let response = await fetch(`/api/patients/${searchId}/record`, { headers: getAuthHeaders() });
      if (response.status === 404) {
        // Not found by ID number — this may be a UHID file number, e.g. a
        // newborn who has no national ID number yet.
        response = await fetch(`/api/patients/uhid/${searchId}/record`, { headers: getAuthHeaders() });
      }
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.message || 'Patient record not found');
      }
      setSearchedPatientRecord(data);
      setAddDiagnosisForm(prev => ({ ...prev, patientId: data.patient.id }));
      setAddPrescriptionForm(prev => ({ ...prev, patientId: data.patient.id }));
      setAddAlertForm(prev => ({ ...prev, patientId: data.patient.id }));
      setAddLabResultForm(prev => ({ ...prev, patientId: data.patient.id }));

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

  // Staff registers new patient (adult, or newborn with birth details)
  const handleRegisterPatient = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');
    setLoading(true);

    try {
      const payload = {
        idNumber: patientRegForm.idNumber,
        firstName: patientRegForm.firstName,
        lastName: patientRegForm.lastName,
        dateOfBirth: patientRegForm.dateOfBirth,
        gender: patientRegForm.gender,
        contactNumber: patientRegForm.contactNumber,
        address: patientRegForm.address,
        email: patientRegForm.email,
        nextOfKinFirstName: patientRegForm.nextOfKinFirstName || undefined,
        nextOfKinLastName: patientRegForm.nextOfKinLastName || undefined,
        nextOfKinRelationship: patientRegForm.nextOfKinRelationship || undefined,
        nextOfKinPhone: patientRegForm.nextOfKinPhone || undefined
      };
      if (isNewbornMode) {
        payload.motherIdNumber = patientRegForm.motherIdNumber || undefined;
        payload.birthFacilityId = userFacilityId ? Number(userFacilityId) : undefined;
        payload.birthWeightGrams = patientRegForm.birthWeightGrams ? parseInt(patientRegForm.birthWeightGrams, 10) : undefined;
        payload.birthLengthCm = patientRegForm.birthLengthCm ? parseFloat(patientRegForm.birthLengthCm) : undefined;
        payload.apgarScore1Min = patientRegForm.apgarScore1Min ? parseInt(patientRegForm.apgarScore1Min, 10) : undefined;
        payload.apgarScore5Min = patientRegForm.apgarScore5Min ? parseInt(patientRegForm.apgarScore5Min, 10) : undefined;
      }

      const response = await fetch('/api/patients', {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(payload)
      });
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.message || 'Registration failed');
      }

      const fileReference = data.idNumber || data.uhid;
      setSuccessMessage(
        data.idNumber
          ? `Patient '${data.firstName} ${data.lastName}' registered successfully!`
          : `Newborn '${data.firstName} ${data.lastName}' registered successfully! No ID number yet — their permanent file number is ${data.uhid}. Note this down; it is the only way to find this file until Home Affairs issues an ID number.`
      );
      setSearchId(fileReference);
      setPatientRegForm({
        idNumber: '', firstName: '', lastName: '', dateOfBirth: '', gender: 'MALE', contactNumber: '', address: '', email: '',
        nextOfKinFirstName: '', nextOfKinLastName: '', nextOfKinRelationship: '', nextOfKinPhone: '',
        motherIdNumber: '', birthWeightGrams: '', birthLengthCm: '', apgarScore1Min: '', apgarScore5Min: ''
      });
      setIsNewbornMode(false);

      if (isNewbornMode) {
        // Re-fetch the full record so the auto-generated EPI schedule shows up immediately.
        const recordRes = await fetch(`/api/patients/uhid/${data.uhid}/record`, { headers: getAuthHeaders() });
        setSearchedPatientRecord(recordRes.ok ? await recordRes.json() : {
          patient: data, allergies: [], chronicConditions: [], visits: [], diagnoses: [], prescriptions: [], labResults: [], immunizations: []
        });
      } else {
        // Load the newly registered patient record
        setSearchedPatientRecord({
          patient: data, allergies: [], chronicConditions: [], visits: [], diagnoses: [], prescriptions: [], labResults: [], immunizations: []
        });
      }
    } catch (err) {
      setErrorMessage(err.message);
    } finally {
      setLoading(false);
    }
  };

  // Generate/refresh a patient's EPI immunization schedule
  const handleGenerateImmunizationSchedule = async (patientId) => {
    setErrorMessage('');
    setSuccessMessage('');
    setLoading(true);
    try {
      const response = await fetch(`/api/immunizations/patient/${patientId}/schedule`, {
        method: 'POST',
        headers: getAuthHeaders()
      });
      if (!response.ok) throw new Error('Failed to generate immunization schedule');
      setSuccessMessage('EPI immunization schedule generated.');
      handleSearchPatient();
    } catch (err) {
      setErrorMessage(err.message);
    } finally {
      setLoading(false);
    }
  };

  // Mark an immunization dose as administered
  const handleAdministerDose = async (immunizationId) => {
    setErrorMessage('');
    setSuccessMessage('');
    setLoading(true);
    try {
      const response = await fetch(`/api/immunizations/${immunizationId}/administer`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({})
      });
      if (!response.ok) throw new Error('Failed to record immunization dose');
      setSuccessMessage('Dose marked as given.');
      handleSearchPatient();
    } catch (err) {
      setErrorMessage(err.message);
    } finally {
      setLoading(false);
    }
  };

  // Mark an immunization dose as missed
  const handleMissDose = async (immunizationId) => {
    setErrorMessage('');
    setSuccessMessage('');
    setLoading(true);
    try {
      const response = await fetch(`/api/immunizations/${immunizationId}/miss`, {
        method: 'POST',
        headers: getAuthHeaders()
      });
      if (!response.ok) throw new Error('Failed to update immunization dose');
      setSuccessMessage('Dose marked as missed.');
      handleSearchPatient();
    } catch (err) {
      setErrorMessage(err.message);
    } finally {
      setLoading(false);
    }
  };

  // Log a manual/catch-up immunization outside the standard EPI schedule
  // (a dose given at another facility, a travel vaccine, etc.)
  const handleAddCatchUpImmunization = async (e, patientId) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');
    setLoading(true);
    try {
      const payload = {
        patientId,
        vaccineName: catchUpForm.vaccineName,
        doseNumber: catchUpForm.doseNumber ? parseInt(catchUpForm.doseNumber, 10) : undefined,
        scheduledDate: catchUpForm.scheduledDate || undefined,
        administeredDate: catchUpForm.administeredDate || undefined,
        notes: catchUpForm.notes || undefined
      };
      const response = await fetch('/api/immunizations', {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(payload)
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.message || 'Failed to add immunization record');
      setSuccessMessage(`Immunization record added: ${data.vaccineName}.`);
      setCatchUpForm({ vaccineName: '', doseNumber: '', scheduledDate: '', administeredDate: '', notes: '' });
      setShowCatchUpForm(false);
      handleSearchPatient();
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
      setSuccessMessage('Prescription added successfully!');
      setAddPrescriptionForm(prev => ({ ...prev, medicationName: '', dosage: '', frequency: 'Once daily', durationDays: 7, notes: '' }));
      handleSearchPatient(); // Refresh record
    } catch (err) {
      setErrorMessage(err.message);
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

  // Staff adds a lab result
  const handleAddLabResult = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');
    setLoading(true);

    try {
      const response = await fetch('/api/lab-results', {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({
          patientId: addLabResultForm.patientId,
          testName: addLabResultForm.testName,
          result: addLabResultForm.result,
          unit: addLabResultForm.unit || undefined,
          normalRange: addLabResultForm.normalRange || undefined,
          notes: addLabResultForm.notes || undefined
        })
      });
      const data = await response.json();
      if (!response.ok) {
        throw new Error(typeof data === 'string' ? data : data.message || 'Failed to add lab result');
      }
      setSuccessMessage('Lab result added successfully!');
      setAddLabResultForm(prev => ({ ...prev, testName: '', result: '', unit: '', normalRange: '', notes: '' }));
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

  const handleSymptomToggle = (id) => {
    if (selectedSymptoms.includes(id)) {
      setSelectedSymptoms(selectedSymptoms.filter(sId => sId !== id));
    } else {
      setSelectedSymptoms([...selectedSymptoms, id]);
    }
  };

  // Map urgency level string to color class
  const getUrgencyBadge = (level) => {
    if (level === 'RED') {
      return <Badge variant="destructive"><ShieldAlert size={14} />🔴 High (Go to Emergency)</Badge>;
    } else if (level === 'YELLOW') {
      return <Badge variant="warning"><AlertTriangle size={14} />🟡 Moderate (Visit Clinic within 24h)</Badge>;
    } else {
      return <Badge variant="success"><CheckCircle size={14} />🟢 Low (Rest & Monitor at Home)</Badge>;
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

  return (
    <div className="min-h-screen bg-background">
    <div className="mx-auto flex max-w-7xl flex-col gap-8 px-4 py-8 md:px-6">
      {/* Header */}
      <header className="flex items-center justify-between border-b pb-5">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary text-primary-foreground">
            <Activity size={22} />
          </div>
          <span className="text-xl font-semibold tracking-tight">Universal Digital Health Record</span>
        </div>
        {token && (
          <div className="flex items-center gap-4">
            <div className="text-right">
              <p className="text-sm font-semibold leading-tight">{userName}</p>
              <p className="text-xs text-muted-foreground">
                {userRole === 'PATIENT' ? 'Patient Portal' : `Staff: ${userRole}`}
              </p>
            </div>
            <Button variant="outline" size="sm" onClick={handleLogout}>
              <LogOut size={16} /> Logout
            </Button>
          </div>
        )}
      </header>

      {/* Main Content Area */}
      <main className="flex min-h-[60vh] flex-col gap-8">

        {/* Alerts */}
        {errorMessage && (
          <Alert variant="destructive" className="flex items-start justify-between gap-3">
            <div className="flex items-start gap-3">
              <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
              <AlertDescription className="text-left">{errorMessage}</AlertDescription>
            </div>
            <Button variant="ghost" size="icon" className="h-6 w-6 shrink-0" onClick={() => setErrorMessage('')}>
              <X className="h-4 w-4" />
            </Button>
          </Alert>
        )}

        {successMessage && (
          <Alert variant="success" className="flex items-start justify-between gap-3">
            <div className="flex items-start gap-3">
              <CheckCircle className="mt-0.5 h-4 w-4 shrink-0" />
              <AlertDescription className="text-left">{successMessage}</AlertDescription>
            </div>
            <Button variant="ghost" size="icon" className="h-6 w-6 shrink-0" onClick={() => setSuccessMessage('')}>
              <X className="h-4 w-4" />
            </Button>
          </Alert>
        )}

        {lowStockAlert && (
          <Alert variant="warning" className="flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-start gap-3">
              <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" />
              <AlertDescription className="text-left">
                {lowStockAlert.items.length} medication{lowStockAlert.items.length > 1 ? 's' : ''} running low at your facility: {lowStockAlert.items.slice(0, 3).map(i => i.medicationName).join(', ')}
                {lowStockAlert.items.length > 3 ? ` and ${lowStockAlert.items.length - 3} more` : ''}.
              </AlertDescription>
            </div>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => { setActiveTabStaff('stock'); fetchStockList(); setLowStockAlert(null); }}
              >
                View Stock
              </Button>
              <Button variant="ghost" size="icon" className="h-6 w-6" onClick={() => setLowStockAlert(null)}>
                <X className="h-4 w-4" />
              </Button>
            </div>
          </Alert>
        )}

        {/* 1. Login Page */}
        {!token && (
          <div className="flex flex-1 items-center justify-center py-6">
            <Card className="w-full max-w-md text-center">
              <CardHeader className="items-center">
                <div className="mb-1 flex h-14 w-14 items-center justify-center rounded-full bg-primary/10 text-primary">
                  <Heart size={28} />
                </div>
                <CardTitle className="text-2xl">Welcome to UDHR</CardTitle>
                <CardDescription>
                  Access your electronic health records, check symptoms, and review guidelines.
                </CardDescription>
              </CardHeader>
              <CardContent>
                <Tabs value={loginRole} onValueChange={(v) => { setLoginRole(v); setErrorMessage(''); }} className="w-full">
                  <TabsList className="mb-6 grid w-full grid-cols-2">
                    <TabsTrigger value="patient">Patient Portal</TabsTrigger>
                    <TabsTrigger value="staff">Healthcare Staff</TabsTrigger>
                  </TabsList>

                  <form onSubmit={handleLogin} className="text-left">
                    {loginRole === 'patient' ? (
                      <>
                        <div className="mb-4 space-y-1.5">
                          <Label htmlFor="patientId">South African ID Number</Label>
                          <Input
                            type="text"
                            id="patientId"
                            value={patientIdNumber}
                            onChange={(e) => setPatientIdNumber(e.target.value)}
                            placeholder="e.g. 9001015000083"
                            required
                          />
                        </div>
                        <div className="mb-4 space-y-1.5">
                          <Label htmlFor="patientDob">Date of Birth</Label>
                          <Input
                            type="date"
                            id="patientDob"
                            value={patientDob}
                            onChange={(e) => setPatientDob(e.target.value)}
                            required
                          />
                        </div>
                        <div className="mb-5 rounded-md border bg-muted/50 p-3">
                          <p className="text-xs text-muted-foreground">
                            💡 <strong className="text-foreground">Demo Patient Login:</strong> Use ID <code>9001015000083</code> and Date of Birth <code>1990-01-01</code> to view the pre-seeded patient (diabetic, hypertensive, penicillin allergic).
                          </p>
                        </div>
                      </>
                    ) : (
                      <>
                        <div className="mb-4 space-y-1.5">
                          <Label htmlFor="staffNum">Staff Number</Label>
                          <Input
                            type="text"
                            id="staffNum"
                            value={staffNumber}
                            onChange={(e) => setStaffNumber(e.target.value)}
                            placeholder="e.g. DOC001 or NUR001"
                            required
                          />
                        </div>
                        <div className="mb-4 space-y-1.5">
                          <Label htmlFor="staffPass">Password</Label>
                          <Input
                            type="password"
                            id="staffPass"
                            value={staffPassword}
                            onChange={(e) => setStaffPassword(e.target.value)}
                            placeholder="••••••••"
                            required
                          />
                        </div>
                        <div className="mb-5 rounded-md border bg-muted/50 p-3">
                          <p className="text-xs text-muted-foreground">
                            💡 <strong className="text-foreground">Demo Staff Logins:</strong><br />
                            - Doctor: <code>DOC001</code> / <code>Doctor@123</code><br />
                            - Nurse: <code>NUR001</code> / <code>Nurse@123</code>
                          </p>
                        </div>
                      </>
                    )}

                    <Button type="submit" className="w-full" disabled={loading}>
                      {loading ? 'Logging in...' : 'Sign In'}
                    </Button>
                  </form>
                </Tabs>
              </CardContent>
            </Card>
          </div>
        )}

        {/* 1b. Forced Password Change (new/admin-created staff account) */}
        {token && mustChangePassword && (
          <div className="flex flex-1 items-center justify-center py-6">
            <Card className="w-full max-w-md text-center">
              <CardHeader className="items-center">
                <div className="mb-1 flex h-14 w-14 items-center justify-center rounded-full bg-amber-500/10 text-amber-600">
                  <ShieldAlert size={28} />
                </div>
                <CardTitle className="text-2xl">Set Your Password</CardTitle>
                <CardDescription>
                  Your account was created with a temporary password by an administrator. Choose a new password before continuing to the staff workspace.
                </CardDescription>
              </CardHeader>
              <CardContent>
                <form onSubmit={handleChangePassword} className="text-left">
                  <div className="mb-4 space-y-1.5">
                    <Label htmlFor="currentPassword">Temporary Password</Label>
                    <Input
                      type="password"
                      id="currentPassword"
                      value={changePasswordForm.currentPassword}
                      onChange={(e) => setChangePasswordForm({...changePasswordForm, currentPassword: e.target.value})}
                      required
                    />
                  </div>
                  <div className="mb-4 space-y-1.5">
                    <Label htmlFor="newPassword">New Password</Label>
                    <Input
                      type="password"
                      id="newPassword"
                      value={changePasswordForm.newPassword}
                      onChange={(e) => setChangePasswordForm({...changePasswordForm, newPassword: e.target.value})}
                      minLength={8}
                      required
                    />
                  </div>
                  <div className="mb-5 space-y-1.5">
                    <Label htmlFor="confirmPassword">Confirm New Password</Label>
                    <Input
                      type="password"
                      id="confirmPassword"
                      value={changePasswordForm.confirmPassword}
                      onChange={(e) => setChangePasswordForm({...changePasswordForm, confirmPassword: e.target.value})}
                      minLength={8}
                      required
                    />
                  </div>
                  <Button type="submit" className="w-full" disabled={loading}>
                    {loading ? 'Updating...' : 'Set Password & Continue'}
                  </Button>
                </form>
              </CardContent>
            </Card>
          </div>
        )}

        {/* 2. Patient Portal View */}
        {token && !mustChangePassword && userRole === 'PATIENT' && (
          <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-[320px_1fr]">

            {/* Sidebar Demographics Card */}
            <div className="flex flex-col gap-6">
              <Card>
                <CardContent className="flex flex-col gap-5 p-6">
                  <div className="flex flex-col items-center gap-1 border-b pb-4 text-center">
                    <div className="mb-2 flex h-16 w-16 items-center justify-center rounded-full bg-primary text-xl font-bold text-primary-foreground">
                      {patientProfile ? patientProfile.firstName.charAt(0) + patientProfile.lastName.charAt(0) : <User />}
                    </div>
                    <h3 className="font-semibold">{patientProfile?.firstName} {patientProfile?.lastName}</h3>
                    <p className="text-sm text-muted-foreground">National Health ID: {patientProfile?.idNumber}</p>
                  </div>

                  <div className="flex flex-col gap-3 text-left text-sm">
                    <div className="flex gap-3">
                      <Calendar size={16} className="mt-0.5 shrink-0 text-muted-foreground" />
                      <div>
                        <p className="text-xs text-muted-foreground">Date of Birth</p>
                        <p>{patientProfile?.dateOfBirth}</p>
                      </div>
                    </div>
                    <div className="flex gap-3">
                      <Compass size={16} className="mt-0.5 shrink-0 text-muted-foreground" />
                      <div>
                        <p className="text-xs text-muted-foreground">Gender</p>
                        <p>{patientProfile?.gender}</p>
                      </div>
                    </div>
                    <div className="flex gap-3">
                      <Phone size={16} className="mt-0.5 shrink-0 text-muted-foreground" />
                      <div>
                        <p className="text-xs text-muted-foreground">Contact Number</p>
                        <p>{patientProfile?.contactNumber || 'Not provided'}</p>
                      </div>
                    </div>
                    <div className="flex gap-3">
                      <MapPin size={16} className="mt-0.5 shrink-0 text-muted-foreground" />
                      <div>
                        <p className="text-xs text-muted-foreground">Address</p>
                        <p className="text-sm">{patientProfile?.address || 'Not provided'}</p>
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>

              {/* Triage History List */}
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2 text-base">
                    <Clock size={18} /> Symptom Check History
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  {triageHistory.length === 0 ? (
                    <p className="text-sm text-muted-foreground">No symptom checks completed yet.</p>
                  ) : (
                    <div className="flex max-h-[350px] flex-col gap-3 overflow-y-auto">
                      {triageHistory.map((check) => (
                        <div key={check.id} className="rounded-lg border bg-muted/40 p-3 text-left">
                          <div className="mb-2 flex items-center justify-between">
                            <span className="text-xs text-muted-foreground">
                              {new Date(check.checkedAt).toLocaleDateString()}
                            </span>
                            <span className={cn(
                              'text-xs font-bold',
                              check.urgencyLevel === 'RED' ? 'text-destructive' : check.urgencyLevel === 'YELLOW' ? 'text-amber-600' : 'text-emerald-600'
                            )}>
                              {check.urgencyLevel}
                            </span>
                          </div>
                          <p className="text-sm">{check.recommendation.split('[')[0]}</p>
                          {check.details && check.details.length > 0 && (
                            <div className="mt-2 flex flex-wrap gap-1.5">
                              {check.details.map((d, idx) => (
                                <span key={idx} className="rounded border bg-background px-2 py-0.5 text-xs text-muted-foreground">
                                  🩺 {d.symptom.name}
                                </span>
                              ))}
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                </CardContent>
              </Card>
            </div>

            {/* Main Portal Panels */}
            <div className="flex flex-col gap-6">

              {/* Dynamic Health Guidance Banner */}
              <Card className="border-l-4 border-l-primary bg-primary/5 text-left">
                <CardContent className="p-6">
                  <h2 className="mb-2 text-xl font-semibold">Personalized Health Guidance Portal</h2>
                  <p className="mb-4 text-sm text-muted-foreground">
                    Based on your active conditions, we have compiled specialized dietary guidelines and safety warnings.
                  </p>

                  {/* Active Tags */}
                  <div className="flex flex-wrap gap-2">
                    {healthGuidance?.conditions.map((c, i) => (
                      <Badge key={i} variant="secondary">Condition: {c}</Badge>
                    ))}
                    {healthGuidance?.allergies.map((a, i) => (
                      <Badge key={i} variant="destructive">⚠️ Allergy: {a}</Badge>
                    ))}
                  </div>
                </CardContent>
              </Card>

              {/* Doctor Clinical Warnings Panel */}
              {patientAlerts && patientAlerts.length > 0 && (
                <Card className="border-l-4 border-l-amber-500 bg-amber-500/5 text-left">
                  <CardContent className="p-6">
                    <h3 className="mb-3 flex items-center gap-2 text-lg font-semibold">
                      <ShieldAlert className="text-amber-600" size={20} /> Clinical Warnings & Doctor's Instructions
                    </h3>
                    <div className="flex flex-col gap-2">
                      {patientAlerts.map((alert) => (
                        <div key={alert.id} className="rounded-lg border bg-background p-3">
                          <div className="mb-1.5 flex items-center justify-between">
                            <span className={cn('text-xs font-bold', alert.severity === 'CRITICAL' ? 'text-destructive' : 'text-amber-600')}>
                              {alert.severity} WARNING
                            </span>
                            <span className="text-xs text-muted-foreground">
                              {new Date(alert.createdAt).toLocaleDateString()}
                            </span>
                          </div>
                          <p className="text-sm leading-relaxed">{alert.message}</p>
                        </div>
                      ))}
                    </div>
                  </CardContent>
                </Card>
              )}

              {/* Drug-Food Conflict Audit Panel (Patient dashboard warning) */}
              {drugFoodConflicts && drugFoodConflicts.length > 0 && (
                <Card className="border-l-4 border-l-destructive bg-destructive/5 text-left">
                  <CardContent className="p-6">
                    <h3 className="mb-3 flex items-center gap-2 text-lg font-semibold">
                      <AlertTriangle className="text-destructive" size={20} /> Active Drug-Food Interactions Flagged
                    </h3>
                    <div className="flex flex-col gap-2">
                      {drugFoodConflicts.map((c, i) => (
                        <div key={i} className="rounded-lg border bg-background p-3">
                          <p className="text-sm"><strong>Medication:</strong> {c.medication} | <strong>Ingredient:</strong> {c.ingredient}</p>
                          <p className={cn('mt-1 text-sm leading-relaxed', c.severity === 'CRITICAL' ? 'text-destructive' : 'text-amber-600')}>{c.message}</p>
                        </div>
                      ))}
                    </div>
                  </CardContent>
                </Card>
              )}

              {/* Medication Reminders & Adherence Widget */}
              <Card className="text-left">
                <CardHeader>
                  <CardTitle className="flex items-center gap-2 text-lg">
                    <BellRing className="text-amber-600" size={20} /> Medication Reminders & Adherence
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  {/* Adherence Compliance Widget */}
                  {reminderData?.stats && reminderData.stats.totalDoses > 0 && (
                    <div className="mb-5 rounded-lg border bg-muted/40 p-4">
                      <div className="mb-2 flex items-center justify-between">
                        <span className="text-sm font-semibold">Weekly Adherence Score</span>
                        <span className={cn(
                          'text-base font-bold',
                          reminderData.stats.adherenceScore >= 80 ? 'text-emerald-600' : reminderData.stats.adherenceScore >= 50 ? 'text-amber-600' : 'text-destructive'
                        )}>
                          {reminderData.stats.adherenceScore}%
                        </span>
                      </div>
                      <div className="mb-2 h-2.5 overflow-hidden rounded-full bg-muted">
                        <div className="h-full bg-primary" style={{ width: `${reminderData.stats.adherenceScore}%` }}></div>
                      </div>
                      <p className="text-xs text-muted-foreground">
                        You have taken {reminderData.stats.takenDoses} out of {reminderData.stats.totalDoses} scheduled doses this week. Keep it up!
                      </p>
                    </div>
                  )}

                  {/* Today's Reminders List */}
                  {!reminderData || reminderData.adherenceLogs.length === 0 ? (
                    <p className="text-sm text-muted-foreground">No medication reminders scheduled for today.</p>
                  ) : (
                    <div className="flex flex-col gap-3">
                      {reminderData.adherenceLogs.map((log) => {
                        const timeString = new Date(log.scheduledTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
                        return (
                          <div
                            key={log.id}
                            className={cn(
                              'flex flex-wrap items-center justify-between gap-3 rounded-lg border p-4',
                              log.status === 'TAKEN' ? 'border-emerald-500/30 bg-emerald-500/5' : log.status === 'MISSED' ? 'border-destructive/30 bg-destructive/5' : 'bg-muted/40'
                            )}
                          >
                            <div>
                              <div className="flex items-center gap-2">
                                <span className="font-semibold">{log.reminder.prescription.medication}</span>
                                <span className="text-sm text-muted-foreground">({log.reminder.prescription.dosage})</span>
                              </div>
                              <p className="mt-1 text-xs text-muted-foreground">
                                Scheduled Time: <strong className="text-foreground">{timeString}</strong> | Frequency: {log.reminder.frequency}
                              </p>
                              {log.takenAt && (
                                <p className="mt-0.5 text-xs text-emerald-600">
                                  Taken at: {new Date(log.takenAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                </p>
                              )}
                              {log.status === 'PENDING' ? (
                                <Input
                                  type="text"
                                  placeholder="How does this make you feel? (e.g. side effects, dizzy...)"
                                  value={adherenceNotes[log.id] || ''}
                                  onChange={(e) => setAdherenceNotes({ ...adherenceNotes, [log.id]: e.target.value })}
                                  className="mt-2 h-8 text-sm"
                                />
                              ) : (
                                log.notes && (
                                  <p className="mt-1.5 text-xs italic text-muted-foreground">
                                    Patient feedback: "{log.notes}"
                                  </p>
                                )
                              )}
                            </div>

                            <div className="flex items-center gap-2">
                              {log.status === 'PENDING' ? (
                                <>
                                  <Button size="sm" className="bg-emerald-600 hover:bg-emerald-600/90" onClick={() => handleUpdateAdherence(log.id, 'TAKEN', adherenceNotes[log.id] || '')}>
                                    <CheckCircle size={14} /> Taken
                                  </Button>
                                  <Button size="sm" variant="destructive" onClick={() => handleUpdateAdherence(log.id, 'MISSED', adherenceNotes[log.id] || '')}>
                                    <XCircle size={14} /> Missed
                                  </Button>
                                </>
                              ) : (
                                <Badge variant={log.status === 'TAKEN' ? 'success' : 'destructive'}>{log.status}</Badge>
                              )}
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  )}
                </CardContent>
              </Card>

              {/* Interactive Symptom Checker */}
              <Card className="text-left">
                <CardHeader>
                  <CardTitle className="flex items-center gap-2 text-lg">
                    <Heart className="text-primary" size={20} /> Symptom Checker & Care Navigator
                  </CardTitle>
                  <CardDescription>
                    Select the symptoms you are currently experiencing. Our care navigation engine (powered by Infermedica) will recommend the appropriate urgency level. <em>Note: This is not a diagnosis.</em>
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  {/* Symptom Checkbox Grid */}
                  <div className="mb-6 grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
                    {symptomsList.map((symptom) => {
                      const isSelected = selectedSymptoms.includes(symptom.id);
                      return (
                        <div
                          key={symptom.id}
                          onClick={() => handleSymptomToggle(symptom.id)}
                          className={cn(
                            'flex cursor-pointer items-center gap-3 rounded-lg border p-3 transition-colors',
                            isSelected ? 'border-primary bg-primary/10' : 'hover:bg-muted/40'
                          )}
                        >
                          <input
                            type="checkbox"
                            checked={isSelected}
                            onChange={() => {}} // Handled by div onClick
                            className="pointer-events-none h-4 w-4 accent-primary"
                          />
                          <div>
                            <p className="text-sm font-medium">{symptom.name}</p>
                            <p className="text-xs text-muted-foreground">ICD-10: {symptom.icd10Code || 'N/A'}</p>
                          </div>
                        </div>
                      );
                    })}
                  </div>

                  <div className="flex justify-end gap-3">
                    {selectedSymptoms.length > 0 && (
                      <Button variant="secondary" onClick={() => setSelectedSymptoms([])}>
                        Clear Selection
                      </Button>
                    )}
                    <Button onClick={handleSymptomCheckSubmit} disabled={selectedSymptoms.length === 0 || loading}>
                      {loading ? 'Analyzing...' : `Analyze ${selectedSymptoms.length} Symptom(s)`}
                    </Button>
                  </div>

                  {/* Triage Recommendation Output Modal */}
                  <Dialog open={!!triageResult} onOpenChange={(open) => { if (!open) setTriageResult(null); }}>
                    <DialogContent className="max-w-lg">
                      {triageResult && (
                        <>
                          <DialogHeader>
                            <div className="flex items-center justify-between gap-3">
                              <DialogTitle>Triage Recommendation</DialogTitle>
                              {getUrgencyBadge(triageResult.urgencyLevel)}
                            </div>
                          </DialogHeader>
                          <p className="text-base font-medium leading-relaxed">
                            {triageResult.recommendation.split('[')[0]}
                          </p>
                          <p className="border-t pt-3 text-xs text-muted-foreground">
                            ⚠️ <strong>Disclaimer:</strong> This tool only provides care recommendations based on symptoms. It does not replace professional medical evaluation. If you feel extremely unwell, seek medical help immediately.
                          </p>
                          <Button className="w-full" onClick={() => setTriageResult(null)}>
                            Acknowledge & Close
                          </Button>
                        </>
                      )}
                    </DialogContent>
                  </Dialog>
                </CardContent>
              </Card>

              {/* Feature 3: Interactive Food Ingredient Checker */}
              <Card className="text-left">
                <CardHeader>
                  <CardTitle className="flex items-center gap-2 text-lg">
                    <FileSpreadsheet className="text-sky-600" size={20} /> Food Ingredient Checker & Safety Scanner
                  </CardTitle>
                  <CardDescription>
                    Input ingredient lists manually, query items via Open Food Facts, or scan labels from packaging photographs (OCR) to evaluate their safety against your medical records.
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  {/* Input Method Selector */}
                  <div className="mb-5 flex max-w-md gap-1 rounded-lg bg-muted p-1">
                    <button
                      className={cn('flex-1 rounded-md px-3 py-1.5 text-sm font-medium transition-colors', foodInputMethod === 'type' ? 'bg-background shadow-sm' : 'text-muted-foreground hover:text-foreground')}
                      onClick={() => setFoodInputMethod('type')}
                    >
                      Type Ingredients
                    </button>
                    <button
                      className={cn('flex-1 rounded-md px-3 py-1.5 text-sm font-medium transition-colors', foodInputMethod === 'search' ? 'bg-background shadow-sm' : 'text-muted-foreground hover:text-foreground')}
                      onClick={() => setFoodInputMethod('search')}
                    >
                      Open Food Facts
                    </button>
                    <button
                      className={cn('flex-1 rounded-md px-3 py-1.5 text-sm font-medium transition-colors', foodInputMethod === 'upload' ? 'bg-background shadow-sm' : 'text-muted-foreground hover:text-foreground')}
                      onClick={() => setFoodInputMethod('upload')}
                    >
                      Upload Label (OCR)
                    </button>
                  </div>

                  {/* Dynamic Inputs based on Selector */}
                  {foodInputMethod === 'type' && (
                    <div className="mb-5 space-y-1.5">
                      <Label>Ingredients List (separate with commas)</Label>
                      <Textarea
                        value={ingredientsInput}
                        onChange={(e) => setIngredientsInput(e.target.value)}
                        placeholder="e.g. Sugar, Wheat Flour, Sodium Chloride, Peanut Butter, Vegetable Fat, Milk..."
                        rows={3}
                      />
                    </div>
                  )}

                  {foodInputMethod === 'search' && (
                    <div className="mb-5 flex flex-col gap-4">
                      <div className="flex gap-3">
                        <div className="w-40 space-y-1.5">
                          <Label>Lookup Type</Label>
                          <Select value={lookupType} onValueChange={setLookupType}>
                            <SelectTrigger><SelectValue /></SelectTrigger>
                            <SelectContent>
                              <SelectItem value="barcode">Barcode</SelectItem>
                              <SelectItem value="search">Product Name</SelectItem>
                            </SelectContent>
                          </Select>
                        </div>
                        <div className="flex-1 space-y-1.5">
                          <Label>{lookupType === 'barcode' ? 'Product Barcode' : 'Search Terms'}</Label>
                          <div className="flex gap-2">
                            <Input
                              type="text"
                              value={productQuery}
                              onChange={(e) => setProductQuery(e.target.value)}
                              placeholder={lookupType === 'barcode' ? 'e.g. 737628064502' : 'e.g. wheat bread'}
                            />
                            <Button variant="secondary" onClick={handleProductLookup} disabled={loading} className="whitespace-nowrap">
                              {lookupType === 'barcode' ? <Barcode size={18} /> : <Search size={18} />} Fetch
                            </Button>
                          </div>
                        </div>
                      </div>
                      {ingredientsInput && (
                        <div className="space-y-1.5">
                          <Label>Fetched Ingredients</Label>
                          <Textarea value={ingredientsInput} onChange={(e) => setIngredientsInput(e.target.value)} rows={2} />
                        </div>
                      )}
                    </div>
                  )}

                  {foodInputMethod === 'upload' && (
                    <div className="mb-5 flex flex-col gap-4">
                      <Label>Upload Food Label Photo</Label>
                      <div className="relative cursor-pointer rounded-lg border-2 border-dashed bg-muted/40 p-6 text-center">
                        <Upload size={32} className="mx-auto mb-2 text-muted-foreground" />
                        <p className="text-sm text-muted-foreground">Choose label file or drag it here</p>
                        <p className="mt-1 text-xs text-muted-foreground">PNG, JPG or JPEG. Max size 5MB.</p>
                        <input
                          type="file"
                          accept="image/*"
                          onChange={handleOcrUpload}
                          className="absolute inset-0 h-full w-full cursor-pointer opacity-0"
                        />
                      </div>
                      {ingredientsInput && (
                        <div className="space-y-1.5">
                          <Label>Extracted Ingredients (OCR Text)</Label>
                          <Textarea value={ingredientsInput} onChange={(e) => setIngredientsInput(e.target.value)} rows={2} />
                        </div>
                      )}
                      <Alert className="border-sky-500/30 bg-sky-500/5">
                        <AlertDescription className="text-xs">
                          💡 <strong>OCR Demo Trigger:</strong> Select any file. If the file name contains <code>juice</code>, <code>chips</code>, or <code>bread</code>, it will automatically extract matching condition-specific ingredients!
                        </AlertDescription>
                      </Alert>
                    </div>
                  )}

                  {ingredientsInput && (
                    <div className="mt-4 flex justify-end">
                      <Button onClick={handleCheckIngredients} disabled={loading}>
                        {loading ? 'Analyzing...' : 'Analyze Safety Profiles'}
                      </Button>
                    </div>
                  )}

                  {/* Analysis Results Display */}
                  {checkResults.length > 0 && (
                    <div className="mt-6 border-t pt-5">
                      <h4 className="mb-4 font-semibold">Scanned Ingredients Analysis</h4>

                      <div className="flex flex-col gap-3">
                        {checkResults.map((res, i) => (
                          <div
                            key={i}
                            className={cn(
                              'flex items-center justify-between gap-3 rounded-lg border p-3',
                              res.status === 'DANGER' ? 'border-destructive/30 bg-destructive/5' : res.status === 'CAUTION' ? 'border-amber-500/30 bg-amber-500/5' : 'border-emerald-500/30 bg-emerald-500/5'
                            )}
                          >
                            <div>
                              <span className="font-semibold">{res.name}</span>
                              <p className={cn('mt-0.5 text-sm', res.status === 'DANGER' ? 'text-destructive' : res.status === 'CAUTION' ? 'text-amber-600' : 'text-emerald-600')}>
                                {res.reason}
                              </p>
                            </div>

                            <Badge variant={res.status === 'DANGER' ? 'destructive' : res.status === 'CAUTION' ? 'warning' : 'success'}>
                              {res.status}
                            </Badge>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </CardContent>
              </Card>

              {/* Health Guidance Details */}
              <div className="grid grid-cols-1 gap-6 md:grid-cols-2">

                {/* Dietary Guidelines Panel */}
                <Card className="text-left">
                  <CardHeader>
                    <CardTitle className="flex items-center gap-2 text-base">
                      <Compass className="text-emerald-600" size={18} /> Personal Dietary Guidelines
                    </CardTitle>
                  </CardHeader>
                  <CardContent>
                    {healthGuidance?.dietaryGuidelines.length === 0 ? (
                      <p className="text-sm text-muted-foreground">No dietary guidelines matching your conditions.</p>
                    ) : (
                      <div className="flex flex-col gap-4">
                        {/* Foods to Eat */}
                        <div>
                          <h4 className="mb-2 flex items-center gap-1.5 text-sm font-semibold text-emerald-600">
                            <CheckCircle size={16} /> Recommended Foods to Eat
                          </h4>
                          <div className="flex flex-col gap-2">
                            {healthGuidance?.dietaryGuidelines.filter(g => g.foodType === 'EAT').map((item) => (
                              <div key={item.id} className="rounded-lg border border-emerald-500/20 bg-emerald-500/5 p-2.5">
                                <p className="text-sm font-semibold">{item.foodItem}</p>
                                <p className="text-sm text-muted-foreground">{item.description}</p>
                                <span className="text-xs text-muted-foreground">Source: {item.source} (ICD-10 Aligned)</span>
                              </div>
                            ))}
                          </div>
                        </div>

                        {/* Foods to Avoid */}
                        <div>
                          <h4 className="mb-2 flex items-center gap-1.5 text-sm font-semibold text-destructive">
                            <XCircle size={16} /> Foods to Strict Limit / Avoid
                          </h4>
                          <div className="flex flex-col gap-2">
                            {healthGuidance?.dietaryGuidelines.filter(g => g.foodType === 'AVOID').map((item) => (
                              <div key={item.id} className="rounded-lg border border-destructive/20 bg-destructive/5 p-2.5">
                                <p className="text-sm font-semibold">{item.foodItem}</p>
                                <p className="text-sm text-muted-foreground">{item.description}</p>
                                <span className="text-xs text-muted-foreground">Source: {item.source} (ICD-10 Aligned)</span>
                              </div>
                            ))}
                          </div>
                        </div>
                      </div>
                    )}
                  </CardContent>
                </Card>

                {/* Health & Lifestyle Tips Panel */}
                <Card className="text-left">
                  <CardHeader>
                    <CardTitle className="flex items-center gap-2 text-base">
                      <Clipboard className="text-amber-600" size={18} /> Lifestyle & Management Tips
                    </CardTitle>
                  </CardHeader>
                  <CardContent>
                    {healthGuidance?.healthTips.length === 0 ? (
                      <p className="text-sm text-muted-foreground">No custom health tips available.</p>
                    ) : (
                      <div className="flex flex-col gap-3">
                        {healthGuidance?.healthTips.map((tip) => (
                          <div key={tip.id} className="rounded-lg border bg-muted/40 p-3">
                            <Badge variant="warning" className="float-right">{tip.tipType}</Badge>
                            <h4 className="mb-1.5 text-sm font-semibold">{tip.title}</h4>
                            <p className="mb-1.5 text-sm text-muted-foreground">{tip.description}</p>
                            <span className="text-xs text-muted-foreground">Source: {tip.source} (SA Dept of Health)</span>
                          </div>
                        ))}
                      </div>
                    )}
                  </CardContent>
                </Card>
              </div>

              {/* FDA Medication Warnings Panel */}
              <Card className="text-left">
                <CardHeader>
                  <CardTitle className="flex items-center gap-2 text-base">
                    <ShieldAlert className="text-destructive" size={18} /> Medication Allergy & OpenFDA Safety Warnings
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  {healthGuidance?.medicationWarnings && Object.keys(healthGuidance.medicationWarnings).length === 0 ? (
                    <p className="text-sm text-muted-foreground">No medication allergies registered.</p>
                  ) : (
                    <div className="flex flex-col gap-5">
                      {healthGuidance?.medicationWarnings && Object.entries(healthGuidance.medicationWarnings).map(([allergen, warnings]) => (
                        <div key={allergen} className="rounded-lg border border-destructive/20 bg-destructive/5 p-4">
                          <div className="mb-3 flex flex-wrap items-center gap-2">
                            <Badge variant="destructive">ALLERGEN: {allergen.toUpperCase()}</Badge>
                            <span className="text-sm text-muted-foreground">Medication cross-reactivity and warnings from OpenFDA:</span>
                          </div>

                          {warnings.length === 0 ? (
                            <p className="text-sm text-muted-foreground">No FDA alerts found for this allergen. Consult your doctor.</p>
                          ) : (
                            <div className="grid grid-cols-1 gap-3">
                              {warnings.map((w, idx) => (
                                <div key={idx} className="rounded-lg border bg-background p-3">
                                  <p className="text-sm font-semibold">
                                    ⚠️ Avoid: <span className="text-destructive">{w.genericName}</span> ({w.brandName})
                                  </p>
                                  <p className="mt-1 rounded-md bg-muted/50 p-2 text-sm italic leading-relaxed text-muted-foreground">
                                    {w.warningText}
                                  </p>
                                </div>
                              ))}
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                </CardContent>
              </Card>
            </div>
          </div>
        )}

        {/* 3. Healthcare Staff View */}
        {token && !mustChangePassword && userRole !== 'PATIENT' && (
          <div>
            {/* Tab Switcher for Staff */}
            <div className="mx-auto mb-6 flex max-w-4xl flex-wrap gap-1 rounded-lg bg-muted p-1">
              <button
                className={cn('flex-1 rounded-md px-3 py-2 text-sm font-medium transition-colors', activeTabStaff === 'patients' ? 'bg-background shadow-sm' : 'text-muted-foreground hover:text-foreground')}
                onClick={() => setActiveTabStaff('patients')}
              >
                Locate & Manage Patients
              </button>
              <button
                className={cn('flex flex-1 items-center justify-center gap-1.5 rounded-md px-3 py-2 text-sm font-medium transition-colors', activeTabStaff === 'queue' ? 'bg-background shadow-sm' : 'text-muted-foreground hover:text-foreground')}
                onClick={() => { setActiveTabStaff('queue'); fetchTodayQueue(); }}
              >
                🕐 Queue {todayQueue && todayQueue.length > 0 && (
                  <Badge className="px-1.5">{todayQueue.length}</Badge>
                )}
              </button>
              <button
                className={cn('flex flex-1 items-center justify-center gap-1.5 rounded-md px-3 py-2 text-sm font-medium transition-colors', activeTabStaff === 'alerts' ? 'bg-background shadow-sm' : 'text-muted-foreground hover:text-foreground')}
                onClick={() => { setActiveTabStaff('alerts'); fetchClinicalAlerts(); }}
              >
                🚨 Clinical Alerts {clinicalAlerts && clinicalAlerts.length > 0 && (
                  <Badge variant="destructive" className="px-1.5">{clinicalAlerts.length}</Badge>
                )}
              </button>
              <button
                className={cn('flex flex-1 items-center justify-center gap-1.5 rounded-md px-3 py-2 text-sm font-medium transition-colors', activeTabStaff === 'referrals' ? 'bg-background shadow-sm' : 'text-muted-foreground hover:text-foreground')}
                onClick={() => { setActiveTabStaff('referrals'); fetchIncomingReferrals(); fetchOutgoingReferrals(); }}
              >
                🔄 Referrals {incomingReferrals.filter(r => r.status === 'PENDING').length > 0 && (
                  <Badge className="px-1.5">{incomingReferrals.filter(r => r.status === 'PENDING').length}</Badge>
                )}
              </button>
              <button
                className={cn('flex flex-1 items-center justify-center gap-1.5 rounded-md px-3 py-2 text-sm font-medium transition-colors', activeTabStaff === 'stock' ? 'bg-background shadow-sm' : 'text-muted-foreground hover:text-foreground')}
                onClick={() => { setActiveTabStaff('stock'); fetchStockList(); }}
              >
                📦 Stock {stockList.filter(s => s.quantityOnHand <= s.reorderLevel).length > 0 && (
                  <Badge variant="destructive" className="px-1.5">{stockList.filter(s => s.quantityOnHand <= s.reorderLevel).length}</Badge>
                )}
              </button>
              {userRole === 'ADMIN' && (
                <button
                  className={cn('flex-1 rounded-md px-3 py-2 text-sm font-medium transition-colors', activeTabStaff === 'staff' ? 'bg-background shadow-sm' : 'text-muted-foreground hover:text-foreground')}
                  onClick={() => { setActiveTabStaff('staff'); fetchStaffList(); fetchFacilitiesList(); fetchAllReports(reportDateRange.startDate, reportDateRange.endDate); }}
                >
                  👥 Staff Management
                </button>
              )}
            </div>

            {activeTabStaff === 'patients' ? (
              <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-[320px_1fr]">

                {/* Search Patient & Register Panel */}
                <div className="flex flex-col gap-6">

                  {/* Search Card */}
                  <Card className="text-left">
                    <CardHeader>
                      <CardTitle className="flex items-center gap-2 text-base">
                        <Search size={18} /> Locate Patient File
                      </CardTitle>
                    </CardHeader>
                    <CardContent>
                      <form onSubmit={handleSearchPatient}>
                        <div className="mb-3 space-y-1.5">
                          <Label htmlFor="searchId">ID Number or File Number (UHID)</Label>
                          <Input
                            type="text"
                            id="searchId"
                            value={searchId}
                            onChange={(e) => setSearchId(e.target.value)}
                            placeholder="ID number, or UDHR-... file number for a newborn"
                            required
                          />
                        </div>
                        <Button type="submit" className="w-full" disabled={loading}>
                          <Search size={16} /> {loading ? 'Searching...' : 'Search Record'}
                        </Button>
                      </form>
                    </CardContent>
                  </Card>

                  {/* Quick Register Card */}
                  <Card className="text-left">
                    <CardHeader>
                      <CardTitle className="flex items-center gap-2 text-base">
                        <PlusCircle size={18} /> Register Patient
                      </CardTitle>
                    </CardHeader>
                    <CardContent>
                      <form onSubmit={handleRegisterPatient}>
                        <div
                          className="mb-4 flex cursor-pointer items-center gap-2.5 rounded-lg border border-sky-500/30 bg-sky-500/5 p-3"
                          onClick={() => setIsNewbornMode(!isNewbornMode)}
                        >
                          <input type="checkbox" checked={isNewbornMode} onChange={() => {}} className="pointer-events-none h-4 w-4 accent-primary" />
                          <div>
                            <p className="text-sm font-semibold">Register a newborn</p>
                            <p className="text-xs text-muted-foreground">No ID number needed yet — opens a file from birth and starts the EPI vaccine schedule.</p>
                          </div>
                        </div>
                        <div className="mb-4 space-y-1.5">
                          <Label>{isNewbornMode ? 'ID Number (leave blank — not yet issued)' : 'ID Number'}</Label>
                          <Input
                            type="text"
                            value={patientRegForm.idNumber}
                            onChange={(e) => setPatientRegForm({...patientRegForm, idNumber: e.target.value})}
                            required={!isNewbornMode}
                            placeholder={isNewbornMode ? 'Leave blank if not yet registered with Home Affairs' : 'SA ID number'}
                          />
                        </div>
                        <div className="mb-4 space-y-1.5">
                          <Label>First Name</Label>
                          <Input
                            type="text"
                            value={patientRegForm.firstName}
                            onChange={(e) => setPatientRegForm({...patientRegForm, firstName: e.target.value})}
                            required
                          />
                        </div>
                        <div className="mb-4 space-y-1.5">
                          <Label>Last Name</Label>
                          <Input
                            type="text"
                            value={patientRegForm.lastName}
                            onChange={(e) => setPatientRegForm({...patientRegForm, lastName: e.target.value})}
                            required
                          />
                        </div>
                        <div className="mb-4 space-y-1.5">
                          <Label>Date of Birth</Label>
                          <Input
                            type="date"
                            value={patientRegForm.dateOfBirth}
                            onChange={(e) => setPatientRegForm({...patientRegForm, dateOfBirth: e.target.value})}
                            required
                          />
                        </div>
                        <div className="mb-4 space-y-1.5">
                          <Label>Gender</Label>
                          <Select value={patientRegForm.gender} onValueChange={(v) => setPatientRegForm({...patientRegForm, gender: v})}>
                            <SelectTrigger><SelectValue /></SelectTrigger>
                            <SelectContent>
                              <SelectItem value="MALE">Male</SelectItem>
                              <SelectItem value="FEMALE">Female</SelectItem>
                              <SelectItem value="OTHER">Other</SelectItem>
                            </SelectContent>
                          </Select>
                        </div>
                        <div className="mb-4 space-y-1.5">
                          <Label>Contact Number</Label>
                          <Input
                            type="text"
                            value={patientRegForm.contactNumber}
                            onChange={(e) => setPatientRegForm({...patientRegForm, contactNumber: e.target.value})}
                          />
                        </div>
                        <div className="mb-4 space-y-1.5">
                          <Label>Email Address</Label>
                          <Input
                            type="email"
                            value={patientRegForm.email}
                            onChange={(e) => setPatientRegForm({...patientRegForm, email: e.target.value})}
                            placeholder="patient@gmail.com"
                          />
                        </div>
                        <div className="mb-4 space-y-1.5">
                          <Label>Address</Label>
                          <Textarea
                            value={patientRegForm.address}
                            onChange={(e) => setPatientRegForm({...patientRegForm, address: e.target.value})}
                            rows={2}
                          />
                        </div>

                        <p className="mb-3 mt-1 text-xs font-semibold text-muted-foreground">Next of Kin</p>
                        <div className="mb-4 grid grid-cols-2 gap-2.5">
                          <div className="space-y-1.5">
                            <Label>First Name</Label>
                            <Input
                              type="text"
                              value={patientRegForm.nextOfKinFirstName}
                              onChange={(e) => setPatientRegForm({...patientRegForm, nextOfKinFirstName: e.target.value})}
                            />
                          </div>
                          <div className="space-y-1.5">
                            <Label>Last Name</Label>
                            <Input
                              type="text"
                              value={patientRegForm.nextOfKinLastName}
                              onChange={(e) => setPatientRegForm({...patientRegForm, nextOfKinLastName: e.target.value})}
                            />
                          </div>
                        </div>
                        <div className="mb-4 grid grid-cols-2 gap-2.5">
                          <div className="space-y-1.5">
                            <Label>Relationship</Label>
                            <Input
                              type="text"
                              value={patientRegForm.nextOfKinRelationship}
                              onChange={(e) => setPatientRegForm({...patientRegForm, nextOfKinRelationship: e.target.value})}
                              placeholder="e.g. Parent, spouse, sibling"
                            />
                          </div>
                          <div className="space-y-1.5">
                            <Label>Phone</Label>
                            <Input
                              type="text"
                              value={patientRegForm.nextOfKinPhone}
                              onChange={(e) => setPatientRegForm({...patientRegForm, nextOfKinPhone: e.target.value})}
                            />
                          </div>
                        </div>

                        {isNewbornMode && (
                          <>
                            <div className="mb-4 space-y-1.5">
                              <Label>Mother's ID Number</Label>
                              <Input
                                type="text"
                                value={patientRegForm.motherIdNumber}
                                onChange={(e) => setPatientRegForm({...patientRegForm, motherIdNumber: e.target.value})}
                                placeholder="Links this file to the mother's record"
                              />
                            </div>
                            <div className="mb-4 grid grid-cols-2 gap-2.5">
                              <div className="space-y-1.5">
                                <Label>Birth Weight (g)</Label>
                                <Input
                                  type="number"
                                  value={patientRegForm.birthWeightGrams}
                                  onChange={(e) => setPatientRegForm({...patientRegForm, birthWeightGrams: e.target.value})}
                                  placeholder="e.g. 3200"
                                />
                              </div>
                              <div className="space-y-1.5">
                                <Label>Birth Length (cm)</Label>
                                <Input
                                  type="number"
                                  step="0.1"
                                  value={patientRegForm.birthLengthCm}
                                  onChange={(e) => setPatientRegForm({...patientRegForm, birthLengthCm: e.target.value})}
                                  placeholder="e.g. 49.5"
                                />
                              </div>
                            </div>
                            <div className="mb-4 grid grid-cols-2 gap-2.5">
                              <div className="space-y-1.5">
                                <Label>Apgar Score (1 min)</Label>
                                <Input
                                  type="number"
                                  min="0"
                                  max="10"
                                  value={patientRegForm.apgarScore1Min}
                                  onChange={(e) => setPatientRegForm({...patientRegForm, apgarScore1Min: e.target.value})}
                                />
                              </div>
                              <div className="space-y-1.5">
                                <Label>Apgar Score (5 min)</Label>
                                <Input
                                  type="number"
                                  min="0"
                                  max="10"
                                  value={patientRegForm.apgarScore5Min}
                                  onChange={(e) => setPatientRegForm({...patientRegForm, apgarScore5Min: e.target.value})}
                                />
                              </div>
                            </div>
                            <Alert className="mb-4 border-sky-500/30 bg-sky-500/5">
                              <AlertDescription className="text-xs">
                                💡 Birth facility is recorded as your current facility. The EPI immunization schedule (BCG, OPV, Rotavirus, PCV...) is generated automatically on save.
                              </AlertDescription>
                            </Alert>
                          </>
                        )}

                        <Button type="submit" variant="secondary" className="w-full">
                          {isNewbornMode ? 'Open Newborn File' : 'Create Record'}
                        </Button>
                      </form>
                    </CardContent>
                  </Card>
                </div>

                {/* Patient File display and clinical actions */}
                <div className="flex flex-col gap-6">
                  {searchedPatientRecord ? (
                    <>
                      {/* Demographics Card */}
                      <Card className="text-left">
                        <CardContent className="flex flex-wrap justify-between gap-5 p-6">
                        <div>
                          <h2 className="text-xl font-semibold">Patient File: {searchedPatientRecord.patient.firstName} {searchedPatientRecord.patient.lastName}</h2>
                          <p className="mt-1 text-sm text-muted-foreground">
                            File Number (UHID): <strong className="text-foreground">{searchedPatientRecord.patient.uhid}</strong>
                            {!searchedPatientRecord.patient.idNumber && (
                              <span className="ml-2 text-amber-600">⚠️ No ID number registered yet</span>
                            )}
                          </p>
                          <p className="mt-1 text-sm text-muted-foreground">
                            ID Number: {searchedPatientRecord.patient.idNumber || 'Not yet issued'} | Gender: {searchedPatientRecord.patient.gender} | DOB: {searchedPatientRecord.patient.dateOfBirth}
                          </p>
                          {searchedPatientRecord.patient.motherPatient && (
                            <p className="mt-1 text-sm text-indigo-600">
                              👶 Mother: {searchedPatientRecord.patient.motherPatient.firstName} {searchedPatientRecord.patient.motherPatient.lastName} (ID: {searchedPatientRecord.patient.motherPatient.idNumber || searchedPatientRecord.patient.motherPatient.uhid})
                            </p>
                          )}
                          {searchedPatientRecord.patient.birthWeightGrams && (
                            <p className="mt-1 text-sm text-muted-foreground">
                              🍼 Born at {searchedPatientRecord.patient.birthFacility?.name || 'N/A'}: {searchedPatientRecord.patient.birthWeightGrams}g, {searchedPatientRecord.patient.birthLengthCm}cm, Apgar {searchedPatientRecord.patient.apgarScore1Min}/{searchedPatientRecord.patient.apgarScore5Min}
                            </p>
                          )}
                          {searchedPatientRecord.patient.nextOfKinFirstName && (
                            <p className="mt-1 text-sm text-muted-foreground">
                              🆘 Next of Kin: {searchedPatientRecord.patient.nextOfKinFirstName} {searchedPatientRecord.patient.nextOfKinLastName}
                              {searchedPatientRecord.patient.nextOfKinRelationship && ` (${searchedPatientRecord.patient.nextOfKinRelationship})`}
                              {searchedPatientRecord.patient.nextOfKinPhone && ` — ${searchedPatientRecord.patient.nextOfKinPhone}`}
                            </p>
                          )}
                          <p className="mt-1 text-sm text-muted-foreground">
                            Contact: {searchedPatientRecord.patient.contactNumber || 'N/A'} | Email: {searchedPatientRecord.patient.email || 'N/A'}
                          </p>
                          <p className="mt-1 text-sm text-muted-foreground">
                            Address: {searchedPatientRecord.patient.address || 'N/A'}
                          </p>
                          {(() => {
                            const lastVisit = searchedPatientRecord.visits && searchedPatientRecord.visits.length > 0
                              ? searchedPatientRecord.visits.reduce((latest, current) =>
                                  new Date(current.visitDate) > new Date(latest.visitDate) ? current : latest
                                )
                              : null;
                            return lastVisit ? (
                              <p className="mt-2 text-sm text-indigo-600">
                                📅 <strong>Last Visit:</strong> {new Date(lastVisit.visitDate).toLocaleDateString()} — <em>{lastVisit.reason} {lastVisit.notes ? `(${lastVisit.notes})` : ''}</em>
                              </p>
                            ) : (
                              <p className="mt-2 text-sm text-muted-foreground">
                                📅 <strong>Last Visit:</strong> No recorded visits
                              </p>
                            );
                          })()}
                        </div>
                        <div className="flex flex-col items-end gap-1.5">
                          <Badge variant="success">Status: Active</Badge>
                          <Button variant="secondary" size="sm" className="mt-1.5" onClick={() => setShowCheckInForm(!showCheckInForm)}>
                            <Clock size={14} /> {showCheckInForm ? 'Cancel Check-In' : 'Check In to Queue'}
                          </Button>
                          <Button variant="secondary" size="sm" onClick={() => setShowReferForm(!showReferForm)}>
                            <FileText size={14} /> {showReferForm ? 'Cancel Referral' : 'Refer to Another Facility'}
                          </Button>
                          <Button variant="secondary" size="sm" onClick={() => setShowDischargeForm(!showDischargeForm)}>
                            <CheckCircle size={14} /> {showDischargeForm ? 'Cancel Discharge' : 'Discharge Patient'}
                          </Button>
                          <Button size="sm" onClick={() => handleEvaluatePatient(searchedPatientRecord.patient.id)}>
                            <RefreshCw size={14} /> Analyze Response (CDS)
                          </Button>
                        </div>
                        </CardContent>
                      </Card>

                      {/* Discharge Patient */}
                      {showDischargeForm && (
                        <Card className="text-left">
                          <CardHeader>
                            <CardTitle className="flex items-center gap-2 text-base">
                              <CheckCircle size={18} /> Discharge {searchedPatientRecord.patient.firstName} {searchedPatientRecord.patient.lastName}
                            </CardTitle>
                          </CardHeader>
                          <CardContent>
                            <form onSubmit={(e) => handleDischarge(e, searchedPatientRecord.patient.id)}>
                              <div className="mb-4 grid grid-cols-2 gap-2.5">
                                <div className="space-y-1.5">
                                  <Label>Outcome</Label>
                                  <Select value={dischargeForm.dischargeOutcome} onValueChange={(v) => setDischargeForm({...dischargeForm, dischargeOutcome: v})}>
                                    <SelectTrigger><SelectValue /></SelectTrigger>
                                    <SelectContent>
                                      <SelectItem value="HOME">Discharged Home</SelectItem>
                                      <SelectItem value="TRANSFERRED">Transferred</SelectItem>
                                      <SelectItem value="ABSCONDED">Absconded</SelectItem>
                                      <SelectItem value="DECEASED">Deceased</SelectItem>
                                    </SelectContent>
                                  </Select>
                                </div>
                                <div className="space-y-1.5">
                                  <Label>Follow-up Date (optional)</Label>
                                  <Input
                                    type="date"
                                    value={dischargeForm.followUpDate}
                                    onChange={(e) => setDischargeForm({...dischargeForm, followUpDate: e.target.value})}
                                  />
                                </div>
                              </div>
                              <div className="mb-4 space-y-1.5">
                                <Label>Discharge Summary</Label>
                                <Textarea
                                  value={dischargeForm.dischargeSummary}
                                  onChange={(e) => setDischargeForm({...dischargeForm, dischargeSummary: e.target.value})}
                                  placeholder="Condition on discharge, instructions given, medication to continue..."
                                  rows={3}
                                />
                              </div>
                              <Button type="submit" className="w-full" disabled={loading}>
                                {loading ? 'Discharging...' : 'Confirm Discharge'}
                              </Button>
                            </form>
                          </CardContent>
                        </Card>
                      )}

                      {/* Refer to Another Facility */}
                      {showReferForm && (
                        <Card className="text-left">
                          <CardHeader>
                            <CardTitle className="flex items-center gap-2 text-base">
                              <FileText size={18} /> Refer {searchedPatientRecord.patient.firstName} {searchedPatientRecord.patient.lastName}
                            </CardTitle>
                          </CardHeader>
                          <CardContent>
                            <form onSubmit={(e) => handleRefer(e, searchedPatientRecord.patient.id)}>
                              <div className="mb-4 grid grid-cols-[2fr_1fr] gap-2.5">
                                <div className="space-y-1.5">
                                  <Label>Destination Facility</Label>
                                  <Select value={referForm.toFacilityId} onValueChange={(v) => setReferForm({...referForm, toFacilityId: v})} required>
                                    <SelectTrigger><SelectValue placeholder="Select facility" /></SelectTrigger>
                                    <SelectContent>
                                      {facilitiesList
                                        .filter(f => String(f.id) !== String(userFacilityId))
                                        .map(f => (
                                          <SelectItem key={f.id} value={String(f.id)}>{f.name} ({f.province})</SelectItem>
                                        ))}
                                    </SelectContent>
                                  </Select>
                                </div>
                                <div className="space-y-1.5">
                                  <Label>Urgency</Label>
                                  <Select value={referForm.urgency} onValueChange={(v) => setReferForm({...referForm, urgency: v})}>
                                    <SelectTrigger><SelectValue /></SelectTrigger>
                                    <SelectContent>
                                      <SelectItem value="ROUTINE">Routine</SelectItem>
                                      <SelectItem value="URGENT">Urgent</SelectItem>
                                      <SelectItem value="EMERGENCY">Emergency</SelectItem>
                                    </SelectContent>
                                  </Select>
                                </div>
                              </div>
                              <div className="mb-4 space-y-1.5">
                                <Label>Reason for Referral</Label>
                                <Input
                                  type="text"
                                  value={referForm.reason}
                                  onChange={(e) => setReferForm({...referForm, reason: e.target.value})}
                                  placeholder="e.g. Requires specialist care beyond this facility's capability"
                                  required
                                />
                              </div>
                              <div className="mb-4 space-y-1.5">
                                <Label>Clinical Summary</Label>
                                <Textarea
                                  value={referForm.clinicalSummary}
                                  onChange={(e) => setReferForm({...referForm, clinicalSummary: e.target.value})}
                                  placeholder="Diagnosis, treatment given, current medications, relevant history..."
                                  rows={3}
                                />
                              </div>
                              <Button type="submit" className="w-full" disabled={loading}>
                                {loading ? 'Referring...' : 'Send Referral'}
                              </Button>
                            </form>
                          </CardContent>
                        </Card>
                      )}

                      {/* Reception: Check In to Queue */}
                      {showCheckInForm && (
                        <Card className="text-left">
                          <CardHeader>
                            <CardTitle className="flex items-center gap-2 text-base">
                              <Clock size={18} /> Check In {searchedPatientRecord.patient.firstName} {searchedPatientRecord.patient.lastName}
                            </CardTitle>
                          </CardHeader>
                          <CardContent>
                            <form onSubmit={(e) => handleCheckIn(e, searchedPatientRecord.patient.id)}>
                              <div className="mb-4 grid grid-cols-2 gap-2.5">
                                <div className="space-y-1.5">
                                  <Label>Department</Label>
                                  <Select value={checkInForm.department} onValueChange={(v) => setCheckInForm({...checkInForm, department: v})}>
                                    <SelectTrigger><SelectValue /></SelectTrigger>
                                    <SelectContent>
                                      <SelectItem value="GP">GP</SelectItem>
                                      <SelectItem value="DENTAL">Dental</SelectItem>
                                      <SelectItem value="MATERNITY">Maternity</SelectItem>
                                      <SelectItem value="PEDIATRICS">Pediatrics</SelectItem>
                                      <SelectItem value="CASUALTY">Casualty</SelectItem>
                                      <SelectItem value="CHRONIC_CLUB">Chronic Medication Club</SelectItem>
                                    </SelectContent>
                                  </Select>
                                </div>
                                <div className="space-y-1.5">
                                  <Label>Urgency</Label>
                                  <Select value={checkInForm.urgency} onValueChange={(v) => setCheckInForm({...checkInForm, urgency: v})}>
                                    <SelectTrigger><SelectValue /></SelectTrigger>
                                    <SelectContent>
                                      <SelectItem value="GREEN">🟢 Green — Routine</SelectItem>
                                      <SelectItem value="YELLOW">🟡 Yellow — Moderate</SelectItem>
                                      <SelectItem value="RED">🔴 Red — Urgent</SelectItem>
                                    </SelectContent>
                                  </Select>
                                </div>
                              </div>
                              <div className="mb-4 space-y-1.5">
                                <Label>Reason for Visit</Label>
                                <Input
                                  type="text"
                                  value={checkInForm.reason}
                                  onChange={(e) => setCheckInForm({...checkInForm, reason: e.target.value})}
                                  placeholder="e.g. Follow-up for hypertension, tooth pain, antenatal check"
                                  required
                                />
                              </div>
                              <Button type="submit" className="w-full" disabled={loading}>
                                {loading ? 'Checking in...' : 'Check In to Queue'}
                              </Button>
                            </form>
                          </CardContent>
                        </Card>
                      )}

                      {/* Treatment Response Timeline (CDS View) */}
                      {patientTimeline && (
                        <Card className="text-left">
                          <CardHeader>
                            <CardTitle className="flex items-center gap-2 text-lg">
                              <Clock className="text-sky-600" size={20} /> 📈 Treatment Response Timeline (Past 14 Days)
                            </CardTitle>
                          </CardHeader>
                          <CardContent>
                          {/* Display active alerts if any */}
                          {patientTimeline.activeAlerts && patientTimeline.activeAlerts.length > 0 && (
                            <div className="mb-4 flex flex-col gap-2">
                              {patientTimeline.activeAlerts.map(alert => (
                                <div key={alert.id} className="rounded-lg border border-destructive/20 bg-destructive/5 p-3">
                                  <p className="text-sm font-semibold text-destructive">🚨 Clinical Alert: {alert.alertType}</p>
                                  <p className="mt-0.5 text-sm">{alert.message}</p>
                                </div>
                              ))}
                            </div>
                          )}

                          {/* Display Food conflicts if any */}
                          {patientTimeline.foodConflicts && patientTimeline.foodConflicts.length > 0 && (
                            <div className="mb-4 rounded-lg border border-amber-500/20 bg-amber-500/5 p-3">
                              <p className="text-sm font-semibold text-amber-600">⚠️ Drug-Food Interactions Detected</p>
                              <div className="mt-1.5 flex flex-col gap-1">
                                {patientTimeline.foodConflicts.map((c, i) => (
                                  <p key={i} className="text-xs">
                                    - <strong>{c.medication}</strong> conflicts with scanned ingredient <strong>{c.ingredient}</strong> ({c.message})
                                  </p>
                                ))}
                              </div>
                            </div>
                          )}

                          {/* Timeline Table Grid */}
                          <div className="grid grid-cols-2 gap-4">
                            <div>
                              <h4 className="mb-2 text-sm font-semibold text-indigo-600">Medication Doses (Adherence logs)</h4>
                              {patientTimeline.adherenceLogs.length === 0 ? (
                                <p className="text-sm text-muted-foreground">No doses logged in last 14 days.</p>
                              ) : (
                                <div className="flex max-h-[200px] flex-col gap-1.5 overflow-y-auto rounded-lg bg-muted/40 p-2">
                                  {patientTimeline.adherenceLogs.map(log => (
                                    <div key={log.id} className="flex flex-col gap-0.5 border-b p-1.5 last:border-b-0">
                                      <div className="flex items-center justify-between text-xs">
                                        <span>{log.reminder.prescription.medication}</span>
                                        <span className="text-muted-foreground">{new Date(log.scheduledTime).toLocaleDateString([], { month: 'short', day: 'numeric' })} {log.reminder.reminderTime}</span>
                                        <Badge variant={log.status === 'TAKEN' ? 'success' : log.status === 'MISSED' ? 'destructive' : 'warning'} className="px-1.5 py-0 text-[0.65rem]">
                                          {log.status}
                                        </Badge>
                                      </div>
                                      {log.notes && (
                                        <p className="text-xs italic text-muted-foreground">
                                          Feedback: "{log.notes}"
                                        </p>
                                      )}
                                    </div>
                                  ))}
                                </div>
                              )}
                            </div>

                            <div>
                              <h4 className="mb-2 text-sm font-semibold text-indigo-600">Symptom Check History</h4>
                              {patientTimeline.symptomChecks.length === 0 ? (
                                <p className="text-sm text-muted-foreground">No symptom checks logged in last 14 days.</p>
                              ) : (
                                <div className="flex max-h-[200px] flex-col gap-1.5 overflow-y-auto rounded-lg bg-muted/40 p-2">
                                  {patientTimeline.symptomChecks.map(check => (
                                    <div key={check.id} className="flex flex-col gap-1 border-b p-1.5 text-xs last:border-b-0">
                                      <div className="flex justify-between">
                                        <span className="text-muted-foreground">{new Date(check.checkedAt).toLocaleDateString()}</span>
                                        <span className={check.urgencyLevel === 'RED' ? 'text-destructive' : check.urgencyLevel === 'YELLOW' ? 'text-amber-600' : 'text-emerald-600'}>
                                          {check.urgencyLevel}
                                        </span>
                                      </div>
                                      <p className="text-xs">{check.recommendation.split('[')[0]}</p>
                                      {check.details && check.details.length > 0 && (
                                        <div className="mt-0.5 flex flex-wrap gap-1">
                                          {check.details.map((d, idx) => (
                                            <span key={idx} className="rounded border bg-background px-1.5 py-0.5 text-[0.65rem] text-muted-foreground">
                                              🩺 {d.symptom.name}
                                            </span>
                                          ))}
                                        </div>
                                      )}
                                    </div>
                                  ))}
                                </div>
                              )}
                            </div>
                          </div>
                          </CardContent>
                        </Card>
                      )}

                      {/* Medical Record sections */}
                      <div className="grid grid-cols-2 gap-6">

                        {/* Active Conditions and Allergies */}
                        <Card className="text-left">
                          <CardHeader>
                            <CardTitle className="flex items-center gap-2 text-base">
                              <Clipboard size={18} /> Chronic Conditions & Allergies
                            </CardTitle>
                          </CardHeader>
                          <CardContent>
                          {/* Conditions list */}
                          <h4 className="mb-2 text-sm font-semibold text-indigo-600">Active Chronic Conditions</h4>
                          {searchedPatientRecord.chronicConditions.length === 0 ? (
                            <p className="mb-4 text-sm text-muted-foreground">No registered chronic conditions.</p>
                          ) : (
                            <div className="mb-4 flex flex-col gap-2">
                              {searchedPatientRecord.chronicConditions.map(c => (
                                <div key={c.id} className="rounded-lg border border-primary/10 bg-primary/5 p-2.5">
                                  <p className="text-sm font-semibold">{c.conditionName}</p>
                                  <p className="text-xs text-muted-foreground">Diagnosed: {c.diagnosedDate} | {c.notes}</p>
                                </div>
                              ))}
                            </div>
                          )}

                          {/* Allergies list */}
                          <h4 className="mb-2 text-sm font-semibold text-destructive">Allergies</h4>
                          {searchedPatientRecord.allergies.length === 0 ? (
                            <p className="text-sm text-muted-foreground">No registered drug or food allergies.</p>
                          ) : (
                            <div className="flex flex-col gap-2">
                              {searchedPatientRecord.allergies.map(a => (
                                <div key={a.id} className="rounded-lg border border-destructive/10 bg-destructive/5 p-2.5">
                                  <p className="text-sm font-semibold">{a.allergen}</p>
                                  <p className="text-xs text-muted-foreground">Severity: {a.severity} | {a.notes}</p>
                                </div>
                              ))}
                            </div>
                          )}
                          </CardContent>
                        </Card>

                        {/* Prescriptions and Adherence */}
                        <div className="flex flex-col gap-5">
                          {/* Active Prescriptions */}
                          <Card className="text-left">
                            <CardHeader>
                              <CardTitle className="flex items-center gap-2 text-base">
                                <Pill size={18} /> Active Prescriptions
                              </CardTitle>
                            </CardHeader>
                            <CardContent>
                            {searchedPatientRecord.prescriptions.length === 0 ? (
                              <p className="text-sm text-muted-foreground">No prescriptions active.</p>
                            ) : (
                              <div className="flex flex-col gap-2">
                                {searchedPatientRecord.prescriptions.map(p => {
                                  const dispenseHistory = (searchedPatientRecord.dispenses || []).filter(d => d.prescription?.id === p.id);
                                  return (
                                  <div key={p.id} className="rounded-lg border bg-muted/40 p-2.5">
                                    <div className="flex flex-wrap items-start justify-between gap-2">
                                      <div>
                                        <p className="text-sm font-semibold">{p.medication}</p>
                                        <p className="text-xs text-muted-foreground">Dosage: {p.dosage} | Frequency: {p.frequency} | Duration: {p.durationDays} days</p>
                                        <p className="mt-0.5 text-xs text-muted-foreground">Notes: {p.notes}</p>
                                      </div>
                                      <div className="flex items-center gap-2">
                                        {dispenseHistory.length > 0 && (
                                          <Badge variant="success">Dispensed x{dispenseHistory.length}</Badge>
                                        )}
                                        {p.active && (
                                          <Button
                                            variant="secondary"
                                            size="sm"
                                            onClick={() => { setDispenseFormFor(dispenseFormFor === p.id ? null : p.id); setDispenseForm({ quantityDispensed: '', daysSupply: '', pharmacyNotes: '' }); }}
                                          >
                                            {dispenseFormFor === p.id ? 'Cancel' : 'Dispense'}
                                          </Button>
                                        )}
                                      </div>
                                    </div>

                                    {dispenseFormFor === p.id && (
                                      <form onSubmit={(e) => handleDispense(e, p.id)} className="mt-3 border-t pt-3">
                                        <div className="mb-3 grid grid-cols-[2fr_1fr] gap-2.5">
                                          <div className="space-y-1.5">
                                            <Label>Quantity Dispensed</Label>
                                            <Input
                                              type="text"
                                              value={dispenseForm.quantityDispensed}
                                              onChange={(e) => setDispenseForm({...dispenseForm, quantityDispensed: e.target.value})}
                                              placeholder="e.g. 30 tablets"
                                              required
                                            />
                                          </div>
                                          <div className="space-y-1.5">
                                            <Label>Days Supply</Label>
                                            <Input
                                              type="number"
                                              value={dispenseForm.daysSupply}
                                              onChange={(e) => setDispenseForm({...dispenseForm, daysSupply: e.target.value})}
                                              placeholder="e.g. 30"
                                            />
                                          </div>
                                        </div>
                                        <div className="mb-3 space-y-1.5">
                                          <Label>Pharmacy Notes</Label>
                                          <Textarea
                                            value={dispenseForm.pharmacyNotes}
                                            onChange={(e) => setDispenseForm({...dispenseForm, pharmacyNotes: e.target.value})}
                                            placeholder="Counselling given, generic substitution, stock notes..."
                                            rows={2}
                                          />
                                        </div>
                                        <Button type="submit" className="w-full" disabled={loading}>
                                          {loading ? 'Recording...' : 'Confirm Dispensed'}
                                        </Button>
                                      </form>
                                    )}

                                    {dispenseHistory.length > 0 && (
                                      <div className="mt-2.5 flex flex-col gap-1.5 border-t pt-2.5">
                                        {dispenseHistory.map(d => (
                                          <div key={d.id} className="text-xs">
                                            <p className="text-muted-foreground">
                                              {d.quantityDispensed}{d.daysSupply ? ` (${d.daysSupply} days)` : ''} — {new Date(d.dispensedAt).toLocaleString()} by {d.dispensedBy?.firstName} {d.dispensedBy?.lastName} at {d.facility?.name}
                                            </p>
                                            {d.pharmacyNotes && <p className="italic text-muted-foreground">{d.pharmacyNotes}</p>}
                                          </div>
                                        ))}
                                      </div>
                                    )}
                                  </div>
                                  );
                                })}
                              </div>
                            )}
                            </CardContent>
                          </Card>

                          {/* Medication Adherence Logs (Doctor view) */}
                          {patientAdherence && patientAdherence.stats.totalDoses > 0 && (
                            <Card className="text-left">
                              <CardHeader>
                                <CardTitle className="flex items-center gap-2 text-base">
                                  <AlertCircle className="text-amber-600" size={18} /> Patient Adherence History
                                </CardTitle>
                              </CardHeader>
                              <CardContent>
                              <div className="mb-4 rounded-lg border bg-muted/40 p-3">
                                <div className="flex items-center justify-between">
                                  <span className="text-sm">Adherence Compliance Score</span>
                                  <span className={cn('text-base font-bold', patientAdherence.stats.adherenceScore >= 80 ? 'text-emerald-600' : 'text-amber-600')}>
                                    {patientAdherence.stats.adherenceScore}%
                                  </span>
                                </div>
                                <p className="mt-1 text-xs text-muted-foreground">
                                  Taken {patientAdherence.stats.takenDoses} of {patientAdherence.stats.totalDoses} doses this week.
                                </p>
                              </div>

                              <div className="flex max-h-[200px] flex-col gap-2 overflow-y-auto">
                                {patientAdherence.adherenceLogs.map((log) => {
                                  const timeString = new Date(log.scheduledTime).toLocaleString([], { dateStyle: 'short', timeStyle: 'short' });
                                  return (
                                    <div key={log.id} className="flex items-center justify-between rounded-lg border bg-muted/30 p-2.5">
                                      <div>
                                        <p className="text-sm font-semibold">{log.reminder.prescription.medication}</p>
                                        <p className="text-xs text-muted-foreground">Scheduled: {timeString}</p>
                                        {log.notes && (
                                          <p className="mt-0.5 text-xs italic text-muted-foreground">
                                            Patient Feedback: "{log.notes}"
                                          </p>
                                        )}
                                      </div>
                                      <Badge variant={log.status === 'TAKEN' ? 'success' : log.status === 'MISSED' ? 'destructive' : 'warning'}>
                                        {log.status}
                                      </Badge>
                                    </div>
                                  );
                                })}
                              </div>
                              </CardContent>
                            </Card>
                          )}
                        </div>
                      </div>

                      {/* Visit History */}
                      <Card className="text-left">
                        <CardHeader>
                          <CardTitle className="flex items-center gap-2 text-base">
                            <Clipboard size={18} /> Visit History
                          </CardTitle>
                        </CardHeader>
                        <CardContent>
                        {(!searchedPatientRecord.visits || searchedPatientRecord.visits.length === 0) ? (
                          <p className="text-sm text-muted-foreground">No visits recorded.</p>
                        ) : (
                          <div className="flex flex-col gap-2.5">
                            {searchedPatientRecord.visits.map(v => (
                              <div key={v.id} className="rounded-lg border bg-muted/40 p-3">
                                <div className="flex flex-wrap items-start justify-between gap-2">
                                  <div>
                                    <p className="text-sm font-semibold">{v.reason}</p>
                                    <p className="text-sm text-muted-foreground">
                                      {new Date(v.visitDate).toLocaleString()} | {v.facility?.name}
                                    </p>
                                  </div>
                                  <Badge variant={v.status === 'ACTIVE' ? 'success' : v.status === 'REFERRED' ? 'warning' : 'destructive'}>
                                    {v.status === 'ACTIVE' ? 'Active' : v.status === 'REFERRED' ? 'Referred' : 'Discharged'}
                                  </Badge>
                                </div>
                                {v.notes && <p className="mt-1.5 text-sm text-muted-foreground">{v.notes}</p>}
                                {v.status === 'DISCHARGED' && (
                                  <div className="mt-2 border-t pt-2">
                                    <p className="text-sm text-emerald-600">
                                      Discharged {v.dischargeOutcome === 'HOME' ? 'home' : v.dischargeOutcome?.toLowerCase()} by {v.dischargedBy?.firstName} {v.dischargedBy?.lastName} on {new Date(v.dischargedAt).toLocaleDateString()}
                                      {v.followUpDate && ` | Follow-up: ${v.followUpDate}`}
                                    </p>
                                    {v.dischargeSummary && <p className="mt-0.5 text-sm text-muted-foreground">{v.dischargeSummary}</p>}
                                  </div>
                                )}
                              </div>
                            ))}
                          </div>
                        )}
                        </CardContent>
                      </Card>

                      {/* Referral History */}
                      <Card className="text-left">
                        <CardHeader>
                          <CardTitle className="flex items-center gap-2 text-base">
                            <FileText size={18} /> Referral History
                          </CardTitle>
                        </CardHeader>
                        <CardContent>
                        {(!searchedPatientRecord.referrals || searchedPatientRecord.referrals.length === 0) ? (
                          <p className="text-sm text-muted-foreground">No referrals on file.</p>
                        ) : (
                          <div className="flex flex-col gap-2.5">
                            {searchedPatientRecord.referrals.map(r => (
                              <div key={r.id} className="rounded-lg border bg-muted/40 p-3">
                                <div className="flex flex-wrap items-start justify-between gap-2">
                                  <div>
                                    <p className="text-sm font-semibold">
                                      {r.fromFacility?.name} → {r.toFacility?.name}
                                    </p>
                                    <p className="text-sm text-muted-foreground">
                                      {r.reason} | {new Date(r.referredAt).toLocaleString()} by {r.referredBy?.firstName} {r.referredBy?.lastName}
                                    </p>
                                  </div>
                                  <div className="flex flex-wrap gap-1.5">
                                    <Badge variant={r.urgency === 'EMERGENCY' ? 'destructive' : r.urgency === 'URGENT' ? 'warning' : 'success'}>
                                      {r.urgency}
                                    </Badge>
                                    <Badge variant={r.status === 'ACCEPTED' || r.status === 'COMPLETED' ? 'success' : r.status === 'DECLINED' || r.status === 'CANCELLED' ? 'destructive' : 'warning'}>
                                      {r.status}
                                    </Badge>
                                  </div>
                                </div>
                                {r.clinicalSummary && <p className="mt-1.5 text-sm text-muted-foreground">{r.clinicalSummary}</p>}
                                {r.responseNotes && (
                                  <p className="mt-1.5 text-sm italic text-muted-foreground">
                                    Response ({r.respondedBy?.firstName} {r.respondedBy?.lastName}): {r.responseNotes}
                                  </p>
                                )}
                              </div>
                            ))}
                          </div>
                        )}
                        </CardContent>
                      </Card>

                      {/* Diagnostic Logs */}
                      <Card className="text-left">
                        <CardHeader>
                          <CardTitle className="flex items-center gap-2 text-base">
                            <FileText size={18} /> Diagnostic Logs & Clinical Visits
                          </CardTitle>
                        </CardHeader>
                        <CardContent>
                        {searchedPatientRecord.diagnoses.length === 0 ? (
                          <p className="text-sm text-muted-foreground">No diagnoses recorded.</p>
                        ) : (
                          <div className="flex flex-col gap-2.5">
                            {searchedPatientRecord.diagnoses.map(d => (
                              <div key={d.id} className="rounded-lg border bg-muted/40 p-3">
                                <p className="text-sm font-semibold">
                                  {d.diagnosis} {d.icd10Code && <span className="ml-1.5 rounded bg-sky-500/10 px-1.5 py-0.5 text-xs font-normal text-sky-600">ICD-10: {d.icd10Code}</span>}
                                </p>
                                <p className="text-sm text-muted-foreground">Diagnosed: {new Date(d.diagnosedAt).toLocaleString()} | Notes: {d.notes}</p>
                              </div>
                            ))}
                          </div>
                        )}
                        </CardContent>
                      </Card>

                      {/* Laboratory Results */}
                      <Card className="text-left">
                        <CardHeader>
                          <CardTitle className="flex items-center gap-2 text-base">
                            <FileSpreadsheet size={18} /> Laboratory Results
                          </CardTitle>
                        </CardHeader>
                        <CardContent>
                        {(!searchedPatientRecord.labResults || searchedPatientRecord.labResults.length === 0) ? (
                          <p className="text-sm text-muted-foreground">No lab results recorded.</p>
                        ) : (
                          <div className="flex flex-col gap-2.5">
                            {searchedPatientRecord.labResults.map(lr => (
                              <div key={lr.id} className="rounded-lg border bg-muted/40 p-3">
                                <div className="flex flex-wrap items-baseline justify-between gap-2">
                                  <p className="text-sm font-semibold">{lr.testName}</p>
                                  <p className="text-base font-bold text-sky-600">
                                    {lr.result} {lr.unit}
                                  </p>
                                </div>
                                <p className="mt-0.5 text-sm text-muted-foreground">
                                  {lr.normalRange && `Normal range: ${lr.normalRange} | `}
                                  Tested: {new Date(lr.testDate).toLocaleString()}
                                </p>
                                {lr.notes && <p className="mt-0.5 text-sm text-muted-foreground">Notes: {lr.notes}</p>}
                              </div>
                            ))}
                          </div>
                        )}
                        </CardContent>
                      </Card>

                      {/* Vitals History */}
                      <Card className="text-left">
                        <CardHeader>
                          <CardTitle className="flex items-center gap-2 text-base">
                            <Heart size={18} /> Vitals History
                          </CardTitle>
                        </CardHeader>
                        <CardContent>
                        {(!searchedPatientRecord.vitals || searchedPatientRecord.vitals.length === 0) ? (
                          <p className="text-sm text-muted-foreground">No vitals recorded. Vitals are captured at check-in from the Queue tab.</p>
                        ) : (
                          <div className="flex flex-col gap-2">
                            {searchedPatientRecord.vitals.map(v => (
                              <div key={v.id} className="rounded-lg border bg-muted/40 p-3">
                                <p className="mb-1.5 text-xs text-muted-foreground">
                                  {new Date(v.recordedAt).toLocaleString()} — recorded by {v.recordedBy?.firstName} {v.recordedBy?.lastName}
                                </p>
                                <div className="flex flex-wrap gap-3 text-sm">
                                  {(v.systolicBp || v.diastolicBp) && <span>🩸 BP: {v.systolicBp}/{v.diastolicBp}</span>}
                                  {v.temperatureC && <span>🌡️ Temp: {v.temperatureC}°C</span>}
                                  {v.pulseBpm && <span>💓 Pulse: {v.pulseBpm} bpm</span>}
                                  {v.respiratoryRate && <span>🫁 Resp: {v.respiratoryRate}/min</span>}
                                  {v.oxygenSaturation && <span>🅾️ SpO2: {v.oxygenSaturation}%</span>}
                                  {v.weightKg && <span>⚖️ Weight: {v.weightKg}kg</span>}
                                  {v.heightCm && <span>📏 Height: {v.heightCm}cm</span>}
                                  {v.bmi && <span>BMI: {v.bmi}</span>}
                                  {v.glucoseMmol && <span>🍬 Glucose: {v.glucoseMmol} mmol/L</span>}
                                </div>
                                {v.notes && <p className="mt-1.5 text-sm text-muted-foreground">Notes: {v.notes}</p>}
                              </div>
                            ))}
                          </div>
                        )}
                        </CardContent>
                      </Card>

                      {/* Immunization Schedule (EPI) */}
                      <Card className="text-left">
                        <CardHeader>
                          <div className="flex flex-wrap items-center justify-between gap-2.5">
                            <CardTitle className="flex items-center gap-2 text-base">
                              <Shield size={18} /> Immunization Schedule (EPI)
                            </CardTitle>
                            <div className="flex gap-2">
                              {(!searchedPatientRecord.immunizations || searchedPatientRecord.immunizations.length === 0) && (
                                <Button
                                  variant="secondary"
                                  size="sm"
                                  onClick={() => handleGenerateImmunizationSchedule(searchedPatientRecord.patient.id)}
                                  disabled={loading}
                                >
                                  Generate EPI Schedule
                                </Button>
                              )}
                              <Button variant="secondary" size="sm" onClick={() => setShowCatchUpForm(!showCatchUpForm)}>
                                {showCatchUpForm ? 'Cancel' : '+ Add Catch-up Record'}
                              </Button>
                            </div>
                          </div>
                        </CardHeader>
                        <CardContent>
                        {showCatchUpForm && (
                          <form
                            onSubmit={(e) => handleAddCatchUpImmunization(e, searchedPatientRecord.patient.id)}
                            className="mb-4 rounded-lg border bg-muted/40 p-4"
                          >
                            <p className="mb-3 text-sm text-muted-foreground">
                              Log a vaccine given outside the standard EPI schedule — a dose administered at another facility before this file existed, a travel vaccine, or a catch-up dose.
                            </p>
                            <div className="mb-3 grid grid-cols-[2fr_1fr] gap-2.5">
                              <div className="space-y-1.5">
                                <Label>Vaccine Name</Label>
                                <Input
                                  type="text"
                                  value={catchUpForm.vaccineName}
                                  onChange={(e) => setCatchUpForm({...catchUpForm, vaccineName: e.target.value})}
                                  placeholder="e.g. Yellow Fever, Hepatitis B booster"
                                  required
                                />
                              </div>
                              <div className="space-y-1.5">
                                <Label>Dose Number</Label>
                                <Input
                                  type="number"
                                  min="0"
                                  value={catchUpForm.doseNumber}
                                  onChange={(e) => setCatchUpForm({...catchUpForm, doseNumber: e.target.value})}
                                  placeholder="optional"
                                />
                              </div>
                            </div>
                            <div className="mb-3 grid grid-cols-2 gap-2.5">
                              <div className="space-y-1.5">
                                <Label>Scheduled Date</Label>
                                <Input
                                  type="date"
                                  value={catchUpForm.scheduledDate}
                                  onChange={(e) => setCatchUpForm({...catchUpForm, scheduledDate: e.target.value})}
                                />
                              </div>
                              <div className="space-y-1.5">
                                <Label>Administered Date (leave blank if still due)</Label>
                                <Input
                                  type="date"
                                  value={catchUpForm.administeredDate}
                                  onChange={(e) => setCatchUpForm({...catchUpForm, administeredDate: e.target.value})}
                                />
                              </div>
                            </div>
                            <div className="mb-3 space-y-1.5">
                              <Label>Notes</Label>
                              <Textarea
                                value={catchUpForm.notes}
                                onChange={(e) => setCatchUpForm({...catchUpForm, notes: e.target.value})}
                                rows={2}
                                placeholder="e.g. Given at Themba Hospital prior to this file being opened"
                              />
                            </div>
                            <Button type="submit" disabled={loading}>
                              {loading ? 'Saving...' : 'Save Record'}
                            </Button>
                          </form>
                        )}

                        {(!searchedPatientRecord.immunizations || searchedPatientRecord.immunizations.length === 0) ? (
                          <p className="text-sm text-muted-foreground">No immunization schedule on file. Generate one to start tracking EPI doses for this patient.</p>
                        ) : (
                          <div className="flex flex-col gap-2">
                            {searchedPatientRecord.immunizations.map(dose => (
                              <div
                                key={dose.id}
                                className={cn(
                                  'flex items-center justify-between rounded-lg border p-2.5',
                                  dose.status === 'GIVEN' ? 'bg-emerald-500/5' : dose.status === 'MISSED' ? 'bg-destructive/5' : 'bg-muted/40'
                                )}
                              >
                                <div>
                                  <p className="text-sm font-semibold">
                                    {dose.vaccineName} {dose.doseNumber != null && `(Dose ${dose.doseNumber})`}
                                  </p>
                                  <p className="text-xs text-muted-foreground">
                                    Scheduled: {dose.scheduledDate}{dose.administeredDate && ` | Given: ${dose.administeredDate}`}
                                  </p>
                                </div>
                                <div className="flex items-center gap-2">
                                  {dose.status === 'DUE' ? (
                                    <>
                                      <Button size="sm" className="bg-emerald-600 hover:bg-emerald-600/90" onClick={() => handleAdministerDose(dose.id)} disabled={loading}>
                                        <CheckCircle size={14} /> Given
                                      </Button>
                                      <Button size="sm" variant="destructive" onClick={() => handleMissDose(dose.id)} disabled={loading}>
                                        <XCircle size={14} /> Missed
                                      </Button>
                                    </>
                                  ) : (
                                    <Badge variant={dose.status === 'GIVEN' ? 'success' : 'destructive'}>{dose.status}</Badge>
                                  )}
                                </div>
                              </div>
                            ))}
                          </div>
                        )}
                        </CardContent>
                      </Card>

                      {/* Clinical Actions Form Panels */}
                      <div className="grid grid-cols-3 gap-5">

                        {/* Add Diagnosis Form */}
                        <Card className="text-left">
                          <CardHeader>
                            <CardTitle className="text-base">Add Clinical Diagnosis</CardTitle>
                          </CardHeader>
                          <CardContent>
                            <form onSubmit={handleAddDiagnosis}>
                              <div className="mb-4 space-y-1.5">
                                <Label>Condition / Disease Name</Label>
                                <Input
                                  type="text"
                                  value={addDiagnosisForm.conditionName}
                                  onChange={(e) => setAddDiagnosisForm({...addDiagnosisForm, conditionName: e.target.value})}
                                  placeholder="e.g. Influenza, Gastritis"
                                  required
                                />
                              </div>
                              <div className="mb-4 space-y-1.5">
                                <Label>Clinical Notes</Label>
                                <Textarea
                                  value={addDiagnosisForm.notes}
                                  onChange={(e) => setAddDiagnosisForm({...addDiagnosisForm, notes: e.target.value})}
                                  placeholder="Patient reports acute onset..."
                                  rows={3}
                                />
                              </div>
                              <Button type="submit" variant="secondary" className="w-full">
                                Save Diagnosis
                              </Button>
                            </form>
                          </CardContent>
                        </Card>

                        {/* Add Prescription Form */}
                        <Card className="text-left">
                          <CardHeader>
                            <CardTitle className="text-base">Issue Prescription</CardTitle>
                          </CardHeader>
                          <CardContent>
                            <form onSubmit={handleAddPrescription}>
                              <div className="mb-4 space-y-1.5">
                                <Label>Medication Name</Label>
                                <Input
                                  type="text"
                                  value={addPrescriptionForm.medicationName}
                                  onChange={(e) => setAddPrescriptionForm({...addPrescriptionForm, medicationName: e.target.value})}
                                  placeholder="e.g. Paracetamol 500mg, Amoxicillin 250mg"
                                  required
                                />
                              </div>
                              <div className="mb-4 grid grid-cols-2 gap-2.5">
                                <div className="space-y-1.5">
                                    <Label>Dosage</Label>
                                    <Input
                                      type="text"
                                      value={addPrescriptionForm.dosage}
                                      onChange={(e) => setAddPrescriptionForm({...addPrescriptionForm, dosage: e.target.value})}
                                      placeholder="e.g. 1 Tablet"
                                      required
                                    />
                                </div>
                                <div className="space-y-1.5">
                                    <Label>Frequency</Label>
                                    <Select value={addPrescriptionForm.frequency} onValueChange={(v) => setAddPrescriptionForm({...addPrescriptionForm, frequency: v})} required>
                                      <SelectTrigger><SelectValue /></SelectTrigger>
                                      <SelectContent>
                                        <SelectItem value="Once daily">Once daily</SelectItem>
                                        <SelectItem value="Twice daily">Twice daily</SelectItem>
                                        <SelectItem value="Three times daily">Three times daily</SelectItem>
                                        <SelectItem value="With meals">With meals</SelectItem>
                                      </SelectContent>
                                    </Select>
                                </div>
                              </div>
                              <div className="mb-4 space-y-1.5">
                                <Label>Duration (Days)</Label>
                                <Input
                                  type="number"
                                  value={addPrescriptionForm.durationDays}
                                  onChange={(e) => setAddPrescriptionForm({...addPrescriptionForm, durationDays: parseInt(e.target.value) || 7})}
                                  required
                                />
                              </div>
                              <Button type="submit" variant="secondary" className="w-full">
                                Issue Prescription
                              </Button>
                            </form>
                          </CardContent>
                        </Card>

                        {/* Add Clinical Alert Form */}
                        <Card className="text-left">
                          <CardHeader>
                            <CardTitle className="text-base">Add Clinical Alert / Warning</CardTitle>
                          </CardHeader>
                          <CardContent>
                            <form onSubmit={handleAddAlert}>
                              <div className="mb-4 space-y-1.5">
                                <Label>Severity Level</Label>
                                <Select value={addAlertForm.severity} onValueChange={(v) => setAddAlertForm({...addAlertForm, severity: v})} required>
                                  <SelectTrigger><SelectValue /></SelectTrigger>
                                  <SelectContent>
                                    <SelectItem value="LOW">Low</SelectItem>
                                    <SelectItem value="MEDIUM">Medium</SelectItem>
                                    <SelectItem value="HIGH">High</SelectItem>
                                    <SelectItem value="CRITICAL">Critical</SelectItem>
                                  </SelectContent>
                                </Select>
                              </div>
                              <div className="mb-4 space-y-1.5">
                                <Label>Alert Message / Instruction</Label>
                                <Textarea
                                  value={addAlertForm.message}
                                  onChange={(e) => setAddAlertForm({...addAlertForm, message: e.target.value})}
                                  placeholder="Patient reports severe dizziness when taking Metformin..."
                                  rows={4}
                                  required
                                />
                              </div>
                              <Button type="submit" variant="secondary" className="w-full">
                                Save Clinical Alert
                              </Button>
                            </form>
                          </CardContent>
                        </Card>
                      </div>

                      {/* Add Lab Result Form */}
                      <Card className="text-left">
                        <CardHeader>
                          <CardTitle className="flex items-center gap-2 text-base">
                            <FileSpreadsheet size={18} /> Add Lab Result
                          </CardTitle>
                        </CardHeader>
                        <CardContent>
                          <form onSubmit={handleAddLabResult}>
                            <div className="mb-4 grid grid-cols-[2fr_1fr_1fr] gap-2.5">
                              <div className="space-y-1.5">
                                <Label>Test Name</Label>
                                <Input
                                  type="text"
                                  value={addLabResultForm.testName}
                                  onChange={(e) => setAddLabResultForm({...addLabResultForm, testName: e.target.value})}
                                  placeholder="e.g. HbA1c, Full Blood Count, GeneXpert MTB/RIF"
                                  required
                                />
                              </div>
                              <div className="space-y-1.5">
                                <Label>Result</Label>
                                <Input
                                  type="text"
                                  value={addLabResultForm.result}
                                  onChange={(e) => setAddLabResultForm({...addLabResultForm, result: e.target.value})}
                                  placeholder="e.g. 6.8"
                                  required
                                />
                              </div>
                              <div className="space-y-1.5">
                                <Label>Unit</Label>
                                <Input
                                  type="text"
                                  value={addLabResultForm.unit}
                                  onChange={(e) => setAddLabResultForm({...addLabResultForm, unit: e.target.value})}
                                  placeholder="e.g. %"
                                />
                              </div>
                            </div>
                            <div className="mb-4 space-y-1.5">
                              <Label>Normal Range</Label>
                              <Input
                                type="text"
                                value={addLabResultForm.normalRange}
                                onChange={(e) => setAddLabResultForm({...addLabResultForm, normalRange: e.target.value})}
                                placeholder="e.g. 4.0 - 5.6%"
                              />
                            </div>
                            <div className="mb-4 space-y-1.5">
                              <Label>Notes</Label>
                              <Textarea
                                value={addLabResultForm.notes}
                                onChange={(e) => setAddLabResultForm({...addLabResultForm, notes: e.target.value})}
                                placeholder="Any interpretation or follow-up notes..."
                                rows={2}
                              />
                            </div>
                            <Button type="submit" variant="secondary" className="w-full" disabled={loading}>
                              {loading ? 'Saving...' : 'Save Lab Result'}
                            </Button>
                          </form>
                        </CardContent>
                      </Card>
                    </>
                  ) : (
                    <Card className="py-20 text-center">
                      <CardContent>
                        <Clipboard size={64} className="mx-auto mb-4 text-muted-foreground opacity-30" />
                        <h3 className="mb-2 font-semibold">No Patient File Loaded</h3>
                        <p className="text-sm text-muted-foreground">Use the lookup tool on the left to locate a patient by their national ID number or register a new patient.</p>
                      </CardContent>
                    </Card>
                  )}
                </div>
              </div>
            ) : activeTabStaff === 'queue' ? (
              /* Reception: Today's Queue */
              <div className="mx-auto mb-10 max-w-3xl px-4 text-left">
                <Card>
                  <CardHeader>
                    <CardTitle className="flex items-center gap-2 text-xl">
                      <Clock className="text-sky-600" /> Today's Queue
                    </CardTitle>
                    <CardDescription>
                      Patients checked in today at your facility, ordered by urgency then arrival time. Check patients in from the "Locate & Manage Patients" tab after finding or registering them.
                    </CardDescription>
                  </CardHeader>
                  <CardContent>
                  {todayQueue.length === 0 ? (
                    <div className="py-14 text-center">
                      <Clock size={48} className="mx-auto mb-3 text-muted-foreground opacity-40" />
                      <h4 className="font-semibold">No one checked in yet</h4>
                      <p className="mt-1 text-sm text-muted-foreground">Check in a patient from their file to see them here.</p>
                    </div>
                  ) : (
                    <div className="flex flex-col gap-3.5">
                      {todayQueue.map(entry => (
                        <div
                          key={entry.id}
                          className={cn(
                            'rounded-xl border p-4.5',
                            entry.urgency === 'RED' ? 'border-destructive/30 bg-destructive/5' : entry.urgency === 'YELLOW' ? 'border-amber-500/30 bg-amber-500/5' : 'bg-muted/40'
                          )}
                        >
                          <div className="flex flex-wrap items-start justify-between gap-3">
                            <div>
                              <span className="text-xs font-semibold text-muted-foreground">
                                #{entry.queueNumber} · {entry.department.replace('_', ' ')}
                              </span>
                              <h3 className="mt-1 text-base font-semibold">
                                {entry.patient.firstName} {entry.patient.lastName}
                              </h3>
                              <p className="mt-0.5 text-sm text-muted-foreground">
                                {entry.patient.idNumber || entry.patient.uhid} | {entry.reason}
                              </p>
                              <p className="mt-1 text-xs text-muted-foreground">
                                Checked in: {new Date(entry.checkedInAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                {entry.calledAt && ` | Called: ${new Date(entry.calledAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`}
                              </p>
                            </div>
                            <div className="flex flex-col items-end gap-2">
                              <select
                                value={entry.urgency}
                                onChange={(e) => handleUpdateQueueUrgency(entry.id, e.target.value)}
                                className="h-8 w-auto rounded-md border border-input bg-transparent px-2 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                              >
                                <option value="GREEN">🟢 Green</option>
                                <option value="YELLOW">🟡 Yellow</option>
                                <option value="RED">🔴 Red</option>
                              </select>
                              <Badge variant={entry.status === 'IN_CONSULTATION' ? 'warning' : 'success'}>
                                {entry.status === 'IN_CONSULTATION' ? 'In Consultation' : 'Waiting'}
                              </Badge>
                            </div>
                          </div>

                          <div className="mt-3.5 flex flex-wrap gap-2 border-t pt-3.5">
                            <Button variant="secondary" size="sm" onClick={() => setVitalsFormFor(vitalsFormFor === entry.id ? null : entry.id)}>
                              {vitalsFormFor === entry.id ? 'Cancel Vitals' : 'Record Vitals'}
                            </Button>
                            {entry.status === 'WAITING' && (
                              <Button size="sm" onClick={() => handleCallIntoConsultation(entry.id)}>
                                Call Into Consultation
                              </Button>
                            )}
                            <Button size="sm" className="bg-emerald-600 hover:bg-emerald-600/90" onClick={() => handleCompleteQueueEntry(entry.id)}>
                              Mark Completed
                            </Button>
                            <Button size="sm" variant="destructive" onClick={() => handleCancelQueueEntry(entry.id)}>
                              Cancel
                            </Button>
                          </div>

                          {vitalsFormFor === entry.id && (
                            <form
                              onSubmit={(e) => handleRecordVitals(e, entry.patient.id, entry.id)}
                              className="mt-3.5 rounded-lg bg-background p-4"
                            >
                              <div className="grid grid-cols-3 gap-2.5">
                                <div className="space-y-1.5">
                                  <Label>Systolic BP</Label>
                                  <Input type="number" value={vitalsForm.systolicBp} onChange={(e) => setVitalsForm({...vitalsForm, systolicBp: e.target.value})} placeholder="mmHg" />
                                </div>
                                <div className="space-y-1.5">
                                  <Label>Diastolic BP</Label>
                                  <Input type="number" value={vitalsForm.diastolicBp} onChange={(e) => setVitalsForm({...vitalsForm, diastolicBp: e.target.value})} placeholder="mmHg" />
                                </div>
                                <div className="space-y-1.5">
                                  <Label>Temperature (°C)</Label>
                                  <Input type="number" step="0.1" value={vitalsForm.temperatureC} onChange={(e) => setVitalsForm({...vitalsForm, temperatureC: e.target.value})} />
                                </div>
                                <div className="space-y-1.5">
                                  <Label>Pulse (bpm)</Label>
                                  <Input type="number" value={vitalsForm.pulseBpm} onChange={(e) => setVitalsForm({...vitalsForm, pulseBpm: e.target.value})} />
                                </div>
                                <div className="space-y-1.5">
                                  <Label>Respiratory Rate</Label>
                                  <Input type="number" value={vitalsForm.respiratoryRate} onChange={(e) => setVitalsForm({...vitalsForm, respiratoryRate: e.target.value})} placeholder="breaths/min" />
                                </div>
                                <div className="space-y-1.5">
                                  <Label>Oxygen Saturation (%)</Label>
                                  <Input type="number" step="0.1" value={vitalsForm.oxygenSaturation} onChange={(e) => setVitalsForm({...vitalsForm, oxygenSaturation: e.target.value})} />
                                </div>
                                <div className="space-y-1.5">
                                  <Label>Weight (kg)</Label>
                                  <Input type="number" step="0.1" value={vitalsForm.weightKg} onChange={(e) => setVitalsForm({...vitalsForm, weightKg: e.target.value})} />
                                </div>
                                <div className="space-y-1.5">
                                  <Label>Height (cm)</Label>
                                  <Input type="number" step="0.1" value={vitalsForm.heightCm} onChange={(e) => setVitalsForm({...vitalsForm, heightCm: e.target.value})} />
                                </div>
                                <div className="space-y-1.5">
                                  <Label>Glucose (mmol/L)</Label>
                                  <Input type="number" step="0.1" value={vitalsForm.glucoseMmol} onChange={(e) => setVitalsForm({...vitalsForm, glucoseMmol: e.target.value})} placeholder="optional" />
                                </div>
                              </div>
                              <div className="mt-3 space-y-1.5">
                                <Label>Notes</Label>
                                <Textarea value={vitalsForm.notes} onChange={(e) => setVitalsForm({...vitalsForm, notes: e.target.value})} rows={2} />
                              </div>
                              <Button type="submit" className="mt-3" disabled={loading}>
                                {loading ? 'Saving...' : 'Save Vitals'}
                              </Button>
                            </form>
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                  </CardContent>
                </Card>
              </div>
            ) : activeTabStaff === 'referrals' ? (
              /* Referrals: Incoming & Outgoing */
              <div className="mx-auto mb-10 max-w-3xl px-4 text-left">
                <Card className="mb-6">
                  <CardHeader>
                    <CardTitle className="text-xl">🔄 Incoming Referrals</CardTitle>
                    <CardDescription>Patients referred to your facility from elsewhere.</CardDescription>
                  </CardHeader>
                  <CardContent>
                  {incomingReferrals.length === 0 ? (
                    <p className="text-sm text-muted-foreground">No incoming referrals.</p>
                  ) : (
                    <div className="flex flex-col gap-3">
                      {incomingReferrals.map(r => (
                        <div
                          key={r.id}
                          className={cn(
                            'rounded-lg border p-4',
                            r.urgency === 'EMERGENCY' ? 'border-destructive/30 bg-destructive/5' : r.urgency === 'URGENT' ? 'border-amber-500/30 bg-amber-500/5' : 'bg-muted/40'
                          )}
                        >
                          <div className="flex flex-wrap items-start justify-between gap-2.5">
                            <div>
                              <h3 className="font-semibold">
                                {r.patient?.firstName} {r.patient?.lastName} <span className="text-sm font-normal text-muted-foreground">({r.patient?.idNumber || r.patient?.uhid})</span>
                              </h3>
                              <p className="mt-0.5 text-sm text-muted-foreground">
                                From {r.fromFacility?.name} | {r.reason}
                              </p>
                              <p className="mt-0.5 text-xs text-muted-foreground">
                                Referred: {new Date(r.referredAt).toLocaleString()} by {r.referredBy?.firstName} {r.referredBy?.lastName}
                              </p>
                            </div>
                            <div className="flex gap-1.5">
                              <Badge variant={r.urgency === 'EMERGENCY' ? 'destructive' : r.urgency === 'URGENT' ? 'warning' : 'success'}>{r.urgency}</Badge>
                              <Badge variant={r.status === 'ACCEPTED' || r.status === 'COMPLETED' ? 'success' : r.status === 'DECLINED' ? 'destructive' : 'warning'}>{r.status}</Badge>
                            </div>
                          </div>
                          {r.clinicalSummary && (
                            <p className="mt-2.5 rounded-md bg-background p-2.5 text-sm text-muted-foreground">
                              {r.clinicalSummary}
                            </p>
                          )}
                          {r.status === 'PENDING' && (
                            <div className="mt-3 flex gap-2">
                              <Button size="sm" className="bg-emerald-600 hover:bg-emerald-600/90" onClick={() => handleRespondToReferral(r.id, 'ACCEPTED')}>
                                Accept
                              </Button>
                              <Button size="sm" variant="destructive" onClick={() => handleRespondToReferral(r.id, 'DECLINED')}>
                                Decline
                              </Button>
                            </div>
                          )}
                          {r.status === 'ACCEPTED' && (
                            <div className="mt-3 flex gap-2">
                              <Button size="sm" className="bg-emerald-600 hover:bg-emerald-600/90" onClick={() => handleRespondToReferral(r.id, 'COMPLETED')}>
                                Mark Seen / Completed
                              </Button>
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader>
                    <CardTitle className="text-xl">📤 Outgoing Referrals</CardTitle>
                    <CardDescription>Patients your facility has referred elsewhere.</CardDescription>
                  </CardHeader>
                  <CardContent>
                  {outgoingReferrals.length === 0 ? (
                    <p className="text-sm text-muted-foreground">No outgoing referrals.</p>
                  ) : (
                    <div className="flex flex-col gap-3">
                      {outgoingReferrals.map(r => (
                        <div key={r.id} className="rounded-lg border bg-muted/40 p-4">
                          <div className="flex flex-wrap items-start justify-between gap-2.5">
                            <div>
                              <h3 className="font-semibold">
                                {r.patient?.firstName} {r.patient?.lastName} <span className="text-sm font-normal text-muted-foreground">({r.patient?.idNumber || r.patient?.uhid})</span>
                              </h3>
                              <p className="mt-0.5 text-sm text-muted-foreground">
                                To {r.toFacility?.name} | {r.reason}
                              </p>
                              <p className="mt-0.5 text-xs text-muted-foreground">
                                Referred: {new Date(r.referredAt).toLocaleString()}
                              </p>
                            </div>
                            <div className="flex gap-1.5">
                              <Badge variant={r.urgency === 'EMERGENCY' ? 'destructive' : r.urgency === 'URGENT' ? 'warning' : 'success'}>{r.urgency}</Badge>
                              <Badge variant={r.status === 'ACCEPTED' || r.status === 'COMPLETED' ? 'success' : r.status === 'DECLINED' ? 'destructive' : 'warning'}>{r.status}</Badge>
                            </div>
                          </div>
                          {r.responseNotes && (
                            <p className="mt-2.5 text-sm italic text-muted-foreground">
                              Response: {r.responseNotes}
                            </p>
                          )}
                          {r.status === 'PENDING' && (
                            <div className="mt-3 flex gap-2">
                              <Button size="sm" variant="destructive" onClick={() => handleRespondToReferral(r.id, 'CANCELLED')}>
                                Cancel Referral
                              </Button>
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                  </CardContent>
                </Card>
              </div>
            ) : activeTabStaff === 'stock' ? (
              /* Pharmacy: Facility Stock/Inventory */
              <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-[320px_1fr]">
                <div className="flex flex-col gap-6">
                  <Card className="text-left">
                    <CardHeader>
                      <CardTitle className="flex items-center gap-2 text-base">
                        <PlusCircle size={18} /> Add Stock Item
                      </CardTitle>
                    </CardHeader>
                    <CardContent>
                      <form onSubmit={handleAddStockItem}>
                        <div className="mb-4 space-y-1.5">
                          <Label>Medication Name</Label>
                          <Input
                            type="text"
                            value={stockItemForm.medicationName}
                            onChange={(e) => setStockItemForm({...stockItemForm, medicationName: e.target.value})}
                            placeholder="e.g. Metformin 500mg"
                            required
                          />
                        </div>
                        <div className="mb-4 grid grid-cols-2 gap-2.5">
                          <div className="space-y-1.5">
                            <Label>Unit</Label>
                            <Input
                              type="text"
                              value={stockItemForm.unit}
                              onChange={(e) => setStockItemForm({...stockItemForm, unit: e.target.value})}
                              placeholder="e.g. tablets"
                            />
                          </div>
                          <div className="space-y-1.5">
                            <Label>Reorder Level</Label>
                            <Input
                              type="number"
                              value={stockItemForm.reorderLevel}
                              onChange={(e) => setStockItemForm({...stockItemForm, reorderLevel: e.target.value})}
                              placeholder="e.g. 20"
                            />
                          </div>
                        </div>
                        <div className="mb-4 space-y-1.5">
                          <Label>Initial Quantity on Hand</Label>
                          <Input
                            type="number"
                            value={stockItemForm.quantityOnHand}
                            onChange={(e) => setStockItemForm({...stockItemForm, quantityOnHand: e.target.value})}
                            placeholder="e.g. 100"
                          />
                        </div>
                        <Button type="submit" variant="secondary" className="w-full" disabled={loading}>
                          {loading ? 'Adding...' : 'Add Stock Item'}
                        </Button>
                      </form>
                    </CardContent>
                  </Card>
                </div>

                <div className="flex flex-col gap-6">
                  <Card className="text-left">
                    <CardHeader>
                      <CardTitle className="flex items-center gap-2 text-base">
                        <Pill size={18} /> Facility Inventory ({stockList.length})
                      </CardTitle>
                    </CardHeader>
                    <CardContent>
                    {stockList.length === 0 ? (
                      <p className="text-sm text-muted-foreground">No medications tracked at your facility yet. Add one to start.</p>
                    ) : (
                      <div className="flex flex-col gap-2.5">
                        {stockList.map(s => {
                          const isLow = s.quantityOnHand <= s.reorderLevel;
                          return (
                          <div
                            key={s.id}
                            className={cn('rounded-lg border p-4', isLow ? 'border-destructive/30 bg-destructive/5' : 'bg-muted/40')}
                          >
                            <div className="flex flex-wrap items-start justify-between gap-2.5">
                              <div>
                                <p className="text-sm font-semibold">{s.medicationName}</p>
                                <p className="text-sm text-muted-foreground">
                                  {s.quantityOnHand} {s.unit} on hand | Reorder at {s.reorderLevel} {s.unit}
                                </p>
                              </div>
                              <div className="flex items-center gap-2">
                                {isLow && <Badge variant="destructive">Low Stock</Badge>}
                                {s.lowStockNotified && (
                                  <Badge variant="warning" title="Admins at this facility have been notified">🔔 Alert Sent</Badge>
                                )}
                                <Button
                                  variant="secondary"
                                  size="sm"
                                  onClick={() => { setReceiveFormFor(receiveFormFor === s.id ? null : s.id); setReceiveForm({ type: 'RECEIVE', quantityChange: '', notes: '' }); }}
                                >
                                  {receiveFormFor === s.id ? 'Cancel' : 'Adjust'}
                                </Button>
                                <Button variant="outline" size="sm" onClick={() => toggleStockHistory(s.id)}>
                                  {stockHistoryFor === s.id ? 'Hide History' : 'History'}
                                </Button>
                              </div>
                            </div>

                            {receiveFormFor === s.id && (
                              <form onSubmit={(e) => handleStockAdjustment(e, s.id)} className="mt-3 border-t pt-3">
                                <div className="mb-3 grid grid-cols-3 gap-2.5">
                                  <div className="space-y-1.5">
                                    <Label>Type</Label>
                                    <Select value={receiveForm.type} onValueChange={(v) => setReceiveForm({...receiveForm, type: v})}>
                                      <SelectTrigger><SelectValue /></SelectTrigger>
                                      <SelectContent>
                                        <SelectItem value="RECEIVE">Receive Stock</SelectItem>
                                        <SelectItem value="ADJUST">Write Off</SelectItem>
                                      </SelectContent>
                                    </Select>
                                  </div>
                                  <div className="space-y-1.5">
                                    <Label>Quantity</Label>
                                    <Input
                                      type="number"
                                      min="1"
                                      value={receiveForm.quantityChange}
                                      onChange={(e) => setReceiveForm({...receiveForm, quantityChange: e.target.value})}
                                      required
                                    />
                                  </div>
                                  <div className="space-y-1.5">
                                    <Label>Notes</Label>
                                    <Input
                                      type="text"
                                      value={receiveForm.notes}
                                      onChange={(e) => setReceiveForm({...receiveForm, notes: e.target.value})}
                                      placeholder={receiveForm.type === 'RECEIVE' ? 'e.g. Delivery ref #1234' : 'e.g. Expired batch'}
                                    />
                                  </div>
                                </div>
                                <Button type="submit" className="w-full" disabled={loading}>
                                  {loading ? 'Saving...' : receiveForm.type === 'RECEIVE' ? 'Confirm Received' : 'Confirm Write-Off'}
                                </Button>
                              </form>
                            )}

                            {stockHistoryFor === s.id && (
                              <div className="mt-2.5 flex flex-col gap-1.5 border-t pt-2.5">
                                {stockHistory.length === 0 ? (
                                  <p className="text-xs text-muted-foreground">No transactions recorded.</p>
                                ) : stockHistory.map(tx => (
                                  <div key={tx.id} className="flex justify-between gap-2.5 text-xs">
                                    <span className="text-muted-foreground">
                                      <Badge variant={tx.type === 'RECEIVED' ? 'success' : tx.type === 'DISPENSED' ? 'warning' : 'destructive'} className="mr-1.5">{tx.type}</Badge>
                                      {tx.quantityChange > 0 ? '+' : ''}{tx.quantityChange} {s.unit} — {new Date(tx.createdAt).toLocaleString()} by {tx.staff?.firstName} {tx.staff?.lastName}
                                      {tx.notes ? ` (${tx.notes})` : ''}
                                    </span>
                                  </div>
                                ))}
                              </div>
                            )}
                          </div>
                          );
                        })}
                      </div>
                    )}
                    </CardContent>
                  </Card>
                </div>
              </div>
            ) : activeTabStaff === 'alerts' ? (
              /* Feature 5: Clinical Alerts Feed Layout */
              <div className="mx-auto mb-10 max-w-6xl px-4 text-left">
                <Card>
                  <CardHeader>
                    <CardTitle className="flex items-center gap-2 text-xl">
                      <ShieldAlert className="text-destructive" /> Active Clinical Alerts & Decision Support Feed
                    </CardTitle>
                    <CardDescription>
                      These alerts are automatically fired by the UDHR engine when a patient shows high medication adherence (≥90%) with poor clinical response (symptoms persisting at Red/Yellow urgency), or high-risk drug-food interactions.
                    </CardDescription>
                  </CardHeader>
                  <CardContent>
                  {clinicalAlerts.length === 0 ? (
                    <div className="py-14 text-center">
                      <CheckCircle size={48} className="mx-auto mb-3 text-emerald-500 opacity-60" />
                      <h4 className="font-semibold">No Active Clinical Alerts</h4>
                      <p className="mt-1 text-sm text-muted-foreground">All monitored patients are responding well to treatment and have no dietary conflicts.</p>
                    </div>
                  ) : (
                    <div className="flex flex-col gap-5">
                      {clinicalAlerts.map((item) => {
                        const alert = item.alert;
                        return (
                          <div
                            key={alert.id}
                            className={cn(
                              'rounded-xl border p-6',
                              alert.severity === 'CRITICAL' ? 'border-destructive/30 bg-destructive/5' : alert.severity === 'HIGH' ? 'border-amber-500/30 bg-amber-500/5' : 'bg-muted/40'
                            )}
                          >
                            {/* Alert Header */}
                            <div className="mb-4 flex flex-wrap items-start justify-between gap-3 border-b pb-4">
                              <div>
                                <Badge variant={alert.severity === 'CRITICAL' ? 'destructive' : 'warning'}>
                                  {alert.severity} SEVERITY
                                </Badge>
                                <h3 className="mt-1.5 text-lg font-semibold">
                                  Patient: {item.patient.firstName} {item.patient.lastName} ({item.patient.idNumber})
                                </h3>
                                <p className="mt-1 text-sm text-muted-foreground">
                                  Fired: {new Date(alert.createdAt).toLocaleString()} | Alert Type: {alert.alertType}
                                </p>
                              </div>
                              <Button className="bg-emerald-600 hover:bg-emerald-600/90" onClick={() => handleResolveAlert(alert.id)}>
                                Resolve Alert & Clear
                              </Button>
                            </div>

                            {/* Alert Details Body */}
                            <p className="mb-4 rounded-lg bg-background p-3 text-sm italic leading-relaxed">
                              {alert.message}
                            </p>

                            {/* Compliance and Symptoms correlation details */}
                            <div className="mb-5 grid grid-cols-2 gap-5">
                              <div>
                                <h4 className="mb-2 text-sm font-semibold text-indigo-600">Patient Adherence & Prescriptions</h4>
                                <p className="text-sm">
                                  Compliance score (last 14 days): <strong>{item.adherenceScore}%</strong>
                                </p>
                                <div className="mt-2 flex flex-wrap gap-1.5">
                                  {item.activePrescriptions.map(p => (
                                    <span key={p.id} className="rounded-md border bg-background px-2 py-1 text-xs">
                                      💊 {p.medication} ({p.dosage})
                                    </span>
                                  ))}
                                </div>
                              </div>

                              <div>
                                <h4 className="mb-2 text-sm font-semibold text-indigo-600">Recent Symptom Checks</h4>
                                {item.recentSymptomChecks.length === 0 ? (
                                  <p className="text-sm text-muted-foreground">No checks logged.</p>
                                ) : (
                                  <div className="flex flex-col gap-1.5">
                                    {item.recentSymptomChecks.map(check => (
                                      <div key={check.id} className="rounded-md bg-background p-1.5 text-xs">
                                        <strong>{new Date(check.checkedAt).toLocaleDateString()}:</strong> Urgency <span className={check.urgencyLevel === 'RED' ? 'text-destructive' : 'text-amber-600'}>{check.urgencyLevel}</span>
                                      </div>
                                    ))}
                                  </div>
                                )}
                              </div>
                            </div>

                            {/* CDSS Diagnostic Recommendations */}
                            {alert.alertType === 'NON_RESPONSE' && (
                              <div className="rounded-lg border bg-background p-4">
                                <h4 className="mb-3 flex items-center gap-1.5 text-sm font-semibold text-sky-600">
                                  <Heart size={16} /> Clinical Decision Support Recommendations
                                </h4>

                                {/* Lab Test suggestion */}
                                {item.labRecommendations && item.labRecommendations.length > 0 && (
                                  <div className="mb-3 border-b pb-3">
                                    <h5 className="text-sm font-semibold">Suggested Laboratory Diagnostics:</h5>
                                    {item.labRecommendations.map(lr => (
                                      <div key={lr.id} className="mt-1.5">
                                        <p className="text-sm text-sky-600">👉 Order: <strong>{lr.testName}</strong> {lr.icdCode && `(ICD-10: ${lr.icdCode})`}</p>
                                        <p className="mt-0.5 text-xs text-muted-foreground"><strong>Reasoning:</strong> {lr.reason}</p>
                                      </div>
                                    ))}
                                  </div>
                                )}

                                {/* Differential Diagnoses suggestions */}
                                {item.differentialDiagnoses && item.differentialDiagnoses.length > 0 && (
                                  <div>
                                    <h5 className="text-sm font-semibold">Suggested ICD-10 Differential Diagnoses:</h5>
                                    <div className="mt-1.5 flex flex-col gap-2">
                                      {item.differentialDiagnoses.map(dd => (
                                        <div key={dd.id} className="rounded-md bg-muted/40 p-2.5">
                                          <div className="flex items-center justify-between">
                                            <span className="text-sm font-semibold">{dd.conditionName} (ICD-10: {dd.icdCode})</span>
                                            <span className={cn(
                                              'rounded px-1.5 py-0.5 text-xs font-bold',
                                              dd.likelihood === 'HIGH' ? 'text-destructive' : dd.likelihood === 'MODERATE' ? 'text-amber-600' : 'text-emerald-600'
                                            )}>
                                              LIKELIHOOD: {dd.likelihood}
                                            </span>
                                          </div>
                                          <p className="mt-1 text-xs text-muted-foreground"><strong>Evidence/Reasoning:</strong> {dd.reasoning}</p>
                                        </div>
                                      ))}
                                    </div>
                                  </div>
                                )}
                              </div>
                            )}
                          </div>
                        );
                      })}
                    </div>
                  )}
                  </CardContent>
                </Card>
              </div>
            ) : (
              /* Admin: Staff Management */
              <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-[320px_1fr]">
                <div className="flex flex-col gap-6">
                  <Card className="text-left">
                    <CardHeader>
                      <CardTitle className="flex items-center gap-2 text-base">
                        <Building2 size={18} /> Register Facility
                      </CardTitle>
                    </CardHeader>
                    <CardContent>
                      <form onSubmit={handleRegisterFacility}>
                        <div className="mb-4 space-y-1.5">
                          <Label>Facility Name</Label>
                          <Input
                            type="text"
                            value={facilityRegForm.name}
                            onChange={(e) => setFacilityRegForm({...facilityRegForm, name: e.target.value})}
                            placeholder="e.g. Themba Hospital"
                            required
                          />
                        </div>
                        <div className="mb-4 space-y-1.5">
                          <Label>Type</Label>
                          <Select value={facilityRegForm.type} onValueChange={(v) => setFacilityRegForm({...facilityRegForm, type: v})}>
                            <SelectTrigger><SelectValue /></SelectTrigger>
                            <SelectContent>
                              <SelectItem value="CLINIC">Clinic</SelectItem>
                              <SelectItem value="HOSPITAL">Hospital</SelectItem>
                            </SelectContent>
                          </Select>
                        </div>
                        <div className="mb-4 space-y-1.5">
                          <Label>Province</Label>
                          <Select value={facilityRegForm.province} onValueChange={(v) => setFacilityRegForm({...facilityRegForm, province: v})}>
                            <SelectTrigger><SelectValue /></SelectTrigger>
                            <SelectContent>
                              {['Eastern Cape', 'Free State', 'Gauteng', 'KwaZulu-Natal', 'Limpopo', 'Mpumalanga', 'North West', 'Northern Cape', 'Western Cape'].map(p => (
                                <SelectItem key={p} value={p}>{p}</SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                        </div>
                        <div className="mb-4 space-y-1.5">
                          <Label>Address</Label>
                          <Textarea
                            value={facilityRegForm.address}
                            onChange={(e) => setFacilityRegForm({...facilityRegForm, address: e.target.value})}
                            placeholder="Street, town/suburb"
                            rows={2}
                            required
                          />
                        </div>
                        <Button type="submit" variant="secondary" className="w-full" disabled={loading}>
                          {loading ? 'Registering...' : 'Register Facility'}
                        </Button>
                      </form>
                    </CardContent>
                  </Card>

                  <Card className="text-left">
                    <CardHeader>
                      <CardTitle className="flex items-center gap-2 text-base">
                        <PlusCircle size={18} /> Register Staff Member
                      </CardTitle>
                    </CardHeader>
                    <CardContent>
                      <form onSubmit={handleRegisterStaff}>
                        <div className="mb-4 space-y-1.5">
                          <Label>Staff Number</Label>
                          <Input
                            type="text"
                            value={staffRegForm.staffNumber}
                            onChange={(e) => setStaffRegForm({...staffRegForm, staffNumber: e.target.value})}
                            placeholder="e.g. NUR002"
                            required
                          />
                        </div>
                        <div className="mb-4 space-y-1.5">
                          <Label>First Name</Label>
                          <Input
                            type="text"
                            value={staffRegForm.firstName}
                            onChange={(e) => setStaffRegForm({...staffRegForm, firstName: e.target.value})}
                            required
                          />
                        </div>
                        <div className="mb-4 space-y-1.5">
                          <Label>Last Name</Label>
                          <Input
                            type="text"
                            value={staffRegForm.lastName}
                            onChange={(e) => setStaffRegForm({...staffRegForm, lastName: e.target.value})}
                            required
                          />
                        </div>
                        <div className="mb-4 space-y-1.5">
                          <Label>Role</Label>
                          <Select value={staffRegForm.role} onValueChange={(v) => setStaffRegForm({...staffRegForm, role: v})}>
                            <SelectTrigger><SelectValue /></SelectTrigger>
                            <SelectContent>
                              <SelectItem value="ADMIN">Admin</SelectItem>
                              <SelectItem value="DOCTOR">Doctor</SelectItem>
                              <SelectItem value="NURSE">Nurse</SelectItem>
                              <SelectItem value="PHARMACIST">Pharmacist</SelectItem>
                            </SelectContent>
                          </Select>
                        </div>
                        <div className="mb-4 space-y-1.5">
                          <Label>Facility</Label>
                          <Select value={staffRegForm.facilityId} onValueChange={(v) => setStaffRegForm({...staffRegForm, facilityId: v})} required>
                            <SelectTrigger><SelectValue placeholder="Select facility" /></SelectTrigger>
                            <SelectContent>
                              {facilitiesList.map(f => (
                                <SelectItem key={f.id} value={String(f.id)}>{f.name} ({f.province})</SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                        </div>
                        <div className="mb-4 space-y-1.5">
                          <Label>Email</Label>
                          <Input
                            type="email"
                            value={staffRegForm.email}
                            onChange={(e) => setStaffRegForm({...staffRegForm, email: e.target.value})}
                            placeholder="staff@udhr.gov.za"
                            required
                          />
                        </div>
                        <div className="mb-4 space-y-1.5">
                          <Label>Contact Number (optional)</Label>
                          <Input
                            type="text"
                            value={staffRegForm.contactNumber}
                            onChange={(e) => setStaffRegForm({...staffRegForm, contactNumber: e.target.value})}
                            placeholder="e.g. 0731234567 — for SMS alerts (admins only)"
                          />
                        </div>
                        <div className="mb-4 space-y-1.5">
                          <Label>Temporary Password</Label>
                          <Input
                            type="password"
                            value={staffRegForm.password}
                            onChange={(e) => setStaffRegForm({...staffRegForm, password: e.target.value})}
                            required
                          />
                        </div>
                        <Button type="submit" variant="secondary" className="w-full" disabled={loading}>
                          {loading ? 'Registering...' : 'Register Staff Member'}
                        </Button>
                      </form>
                    </CardContent>
                  </Card>
                </div>

                <div className="flex flex-col gap-6">
                  <Card className="text-left">
                    <CardHeader>
                      <CardTitle className="flex items-center gap-2 text-base">
                        <Calendar size={18} /> Report Date Range
                      </CardTitle>
                    </CardHeader>
                    <CardContent>
                    <div className="flex flex-wrap items-end gap-3">
                      <div className="space-y-1.5">
                        <Label>Start Date</Label>
                        <Input
                          type="date"
                          value={reportDateRange.startDate}
                          onChange={(e) => setReportDateRange({...reportDateRange, startDate: e.target.value})}
                        />
                      </div>
                      <div className="space-y-1.5">
                        <Label>End Date</Label>
                        <Input
                          type="date"
                          value={reportDateRange.endDate}
                          onChange={(e) => setReportDateRange({...reportDateRange, endDate: e.target.value})}
                        />
                      </div>
                      <Button onClick={() => fetchAllReports(reportDateRange.startDate, reportDateRange.endDate)}>
                        Apply
                      </Button>
                      <Button
                        variant="outline"
                        onClick={() => { setReportDateRange({ startDate: '', endDate: '' }); fetchAllReports('', ''); }}
                      >
                        All Time
                      </Button>
                      <Button
                        variant="outline"
                        onClick={() => {
                          const end = new Date();
                          const start = new Date();
                          start.setDate(end.getDate() - 6);
                          const fmt = (d) => d.toISOString().split('T')[0];
                          setReportDateRange({ startDate: fmt(start), endDate: fmt(end) });
                          fetchAllReports(fmt(start), fmt(end));
                        }}
                      >
                        Last 7 Days
                      </Button>
                      <Button
                        variant="outline"
                        onClick={() => {
                          const end = new Date();
                          const start = new Date();
                          start.setDate(end.getDate() - 29);
                          const fmt = (d) => d.toISOString().split('T')[0];
                          setReportDateRange({ startDate: fmt(start), endDate: fmt(end) });
                          fetchAllReports(fmt(start), fmt(end));
                        }}
                      >
                        Last 30 Days
                      </Button>
                    </div>
                    <p className="mt-2.5 text-xs text-muted-foreground">
                      Applies to every report below. "Today" tiles (e.g. Dispensed Today) always reflect the current day regardless of this filter. Stock's currently-tracked and low-stock counts always reflect live inventory.
                    </p>
                    </CardContent>
                  </Card>

                  {stockReport && (
                    <Card className="text-left">
                      <CardHeader>
                        <div className="flex flex-wrap items-start justify-between gap-2.5">
                          <CardTitle className="flex items-center gap-2 text-lg">
                            <Pill size={18} /> Pharmacy Stock Report
                          </CardTitle>
                          <div className="flex gap-2">
                            <Button variant="outline" size="sm" onClick={() => downloadReportCsv('/api/stock/report/export', 'stock-transactions.csv')}>
                              <Download size={14} /> Export CSV
                            </Button>
                            <Button variant="outline" size="sm" onClick={exportStockPdf}>
                              <FileText size={14} /> Export PDF
                            </Button>
                          </div>
                        </div>
                        <CardDescription>{stockReport.facilityName}</CardDescription>
                      </CardHeader>
                      <CardContent>
                      <div className="mb-4 grid grid-cols-3 gap-2.5">
                        <div className="rounded-xl border bg-muted/40 p-3">
                          <p className="text-xs text-muted-foreground">Medications Tracked</p>
                          <p className="text-xl font-bold">{stockReport.totalMedicationsTracked}</p>
                        </div>
                        <div className={cn('rounded-xl border p-3', stockReport.lowStockCount > 0 ? 'border-destructive/30 bg-destructive/5' : 'bg-muted/40')}>
                          <p className="text-xs text-muted-foreground">Low Stock Items</p>
                          <p className={cn('text-xl font-bold', stockReport.lowStockCount > 0 && 'text-destructive')}>{stockReport.lowStockCount}</p>
                        </div>
                        <div className="rounded-xl border bg-muted/40 p-3">
                          <p className="text-xs text-muted-foreground">Units Received ({reportRangeSuffix()})</p>
                          <p className="text-xl font-bold text-emerald-600">+{stockReport.totalUnitsReceived}</p>
                        </div>
                        <div className="rounded-xl border bg-muted/40 p-3">
                          <p className="text-xs text-muted-foreground">Units Dispensed ({reportRangeSuffix()})</p>
                          <p className="text-xl font-bold text-sky-600">-{stockReport.totalUnitsDispensed}</p>
                        </div>
                        <div className="rounded-xl border bg-muted/40 p-3">
                          <p className="text-xs text-muted-foreground">Units Written Off ({reportRangeSuffix()})</p>
                          <p className="text-xl font-bold text-amber-600">-{stockReport.totalUnitsWrittenOff}</p>
                        </div>
                      </div>

                      {stockReport.lowStockItems.length > 0 && (
                        <div className="mb-4">
                          <p className="mb-2 text-sm font-semibold">Needs Reordering</p>
                          <div className="flex flex-col gap-1.5">
                            {stockReport.lowStockItems.map(item => (
                              <div key={item.id} className="flex items-center justify-between rounded-lg border border-destructive/20 bg-destructive/5 px-3 py-2">
                                <span className="text-sm">{item.medicationName}</span>
                                <div className="flex items-center gap-1.5">
                                  {item.lowStockNotified && <Badge variant="warning">🔔 Alert Sent</Badge>}
                                  <Badge variant="destructive">{item.quantityOnHand} / {item.reorderLevel} {item.unit}</Badge>
                                </div>
                              </div>
                            ))}
                          </div>
                        </div>
                      )}

                      <div>
                        <p className="mb-2 text-sm font-semibold">Recent Stock Activity</p>
                        {stockReport.recentTransactions.length === 0 ? (
                          <p className="text-sm text-muted-foreground">No stock activity recorded yet.</p>
                        ) : (
                          <div className="flex flex-col gap-1.5">
                            {stockReport.recentTransactions.map(tx => (
                              <div key={tx.id} className="text-sm">
                                <Badge variant={tx.type === 'RECEIVED' ? 'success' : tx.type === 'DISPENSED' ? 'warning' : 'destructive'} className="mr-1.5">{tx.type}</Badge>
                                <span className="text-muted-foreground">
                                  {tx.stockItem?.medicationName}: {tx.quantityChange > 0 ? '+' : ''}{tx.quantityChange} {tx.stockItem?.unit} — {new Date(tx.createdAt).toLocaleString()} by {tx.staff?.firstName} {tx.staff?.lastName}
                                  {tx.notes ? ` (${tx.notes})` : ''}
                                </span>
                              </div>
                            ))}
                          </div>
                        )}
                      </div>
                      </CardContent>
                    </Card>
                  )}

                  {dispenseReport && (
                    <Card className="text-left">
                      <CardHeader>
                        <div className="flex flex-wrap items-start justify-between gap-2.5">
                          <CardTitle className="flex items-center gap-2 text-lg">
                            <FileText size={18} /> Pharmacy Dispensing Report
                          </CardTitle>
                          <div className="flex gap-2">
                            <Button variant="outline" size="sm" onClick={() => downloadReportCsv('/api/dispensing/report/export', 'dispensing.csv')}>
                              <Download size={14} /> Export CSV
                            </Button>
                            <Button variant="outline" size="sm" onClick={exportDispensePdf}>
                              <FileText size={14} /> Export PDF
                            </Button>
                          </div>
                        </div>
                        <CardDescription>{dispenseReport.facilityName}</CardDescription>
                      </CardHeader>
                      <CardContent>
                      <div className="mb-4 grid grid-cols-3 gap-2.5">
                        <div className="rounded-xl border bg-muted/40 p-3">
                          <p className="text-xs text-muted-foreground">Total Dispense Events</p>
                          <p className="text-xl font-bold">{dispenseReport.totalDispenseEvents}</p>
                        </div>
                        <div className="rounded-xl border bg-muted/40 p-3">
                          <p className="text-xs text-muted-foreground">Dispensed Today</p>
                          <p className="text-xl font-bold text-sky-600">{dispenseReport.dispensedToday}</p>
                        </div>
                        <div className="rounded-xl border bg-muted/40 p-3">
                          <p className="text-xs text-muted-foreground">Unique Patients Served</p>
                          <p className="text-xl font-bold">{dispenseReport.uniquePatientsServed}</p>
                        </div>
                      </div>

                      {dispenseReport.topMedications.length > 0 && (
                        <div className="mb-4">
                          <p className="mb-2 text-sm font-semibold">Top Dispensed Medications</p>
                          <div className="flex flex-col gap-1.5">
                            {dispenseReport.topMedications.map((m, idx) => (
                              <div key={m.medicationName} className="flex items-center justify-between rounded-lg border bg-muted/40 px-3 py-2">
                                <span className="text-sm">#{idx + 1} {m.medicationName}</span>
                                <span className="text-sm text-muted-foreground">
                                  {m.dispenseCount} dispense{m.dispenseCount !== 1 ? 's' : ''}{m.totalUnitsDispensed > 0 ? ` · ${m.totalUnitsDispensed} units` : ''}
                                </span>
                              </div>
                            ))}
                          </div>
                        </div>
                      )}

                      <div>
                        <p className="mb-2 text-sm font-semibold">Recent Dispensing Activity</p>
                        {dispenseReport.recentDispenses.length === 0 ? (
                          <p className="text-sm text-muted-foreground">No dispensing activity recorded yet.</p>
                        ) : (
                          <div className="flex flex-col gap-1.5">
                            {dispenseReport.recentDispenses.map(d => (
                              <div key={d.id} className="text-sm text-muted-foreground">
                                <span className="text-foreground">{d.prescription?.medication}</span> ({d.quantityDispensed}) to {d.patient?.firstName} {d.patient?.lastName} — {new Date(d.dispensedAt).toLocaleString()} by {d.dispensedBy?.firstName} {d.dispensedBy?.lastName}
                              </div>
                            ))}
                          </div>
                        )}
                      </div>
                      </CardContent>
                    </Card>
                  )}

                  {referralReport && (
                    <Card className="text-left">
                      <CardHeader>
                        <div className="flex flex-wrap items-start justify-between gap-2.5">
                          <CardTitle className="flex items-center gap-2 text-lg">
                            <RefreshCw size={18} /> Facility Referral Report
                          </CardTitle>
                          <div className="flex gap-2">
                            <Button variant="outline" size="sm" onClick={() => downloadReportCsv('/api/referrals/report/export', 'referrals.csv')}>
                              <Download size={14} /> Export CSV
                            </Button>
                            <Button variant="outline" size="sm" onClick={exportReferralPdf}>
                              <FileText size={14} /> Export PDF
                            </Button>
                          </div>
                        </div>
                        <CardDescription>{referralReport.facilityName}</CardDescription>
                      </CardHeader>
                      <CardContent>
                      <div className="mb-4 grid grid-cols-3 gap-2.5">
                        <div className="rounded-xl border bg-muted/40 p-3">
                          <p className="text-xs text-muted-foreground">Outgoing Referrals ({reportRangeSuffix()})</p>
                          <p className="text-xl font-bold">{referralReport.totalOutgoing}</p>
                        </div>
                        <div className="rounded-xl border bg-muted/40 p-3">
                          <p className="text-xs text-muted-foreground">Incoming Referrals ({reportRangeSuffix()})</p>
                          <p className="text-xl font-bold">{referralReport.totalIncoming}</p>
                        </div>
                        <div className={cn('rounded-xl border p-3', referralReport.pendingIncoming > 0 ? 'border-amber-500/30 bg-amber-500/5' : 'bg-muted/40')}>
                          <p className="text-xs text-muted-foreground">Pending Incoming (Needs Response)</p>
                          <p className={cn('text-xl font-bold', referralReport.pendingIncoming > 0 && 'text-amber-600')}>{referralReport.pendingIncoming}</p>
                        </div>
                        <div className={cn('rounded-xl border p-3', referralReport.emergencyReferrals > 0 ? 'border-destructive/30 bg-destructive/5' : 'bg-muted/40')}>
                          <p className="text-xs text-muted-foreground">Emergency Referrals ({reportRangeSuffix()})</p>
                          <p className={cn('text-xl font-bold', referralReport.emergencyReferrals > 0 && 'text-destructive')}>{referralReport.emergencyReferrals}</p>
                        </div>
                      </div>

                      <div className="mb-4 grid grid-cols-2 gap-4">
                        {referralReport.topDestinations.length > 0 && (
                          <div>
                            <p className="mb-2 text-sm font-semibold">Top Destination Facilities</p>
                            <div className="flex flex-col gap-1.5">
                              {referralReport.topDestinations.map(f => (
                                <div key={f.facilityName} className="flex items-center justify-between rounded-lg border bg-muted/40 px-3 py-2">
                                  <span className="text-sm">{f.facilityName}</span>
                                  <span className="text-sm text-muted-foreground">{f.referralCount}</span>
                                </div>
                              ))}
                            </div>
                          </div>
                        )}
                        {referralReport.topSources.length > 0 && (
                          <div>
                            <p className="mb-2 text-sm font-semibold">Top Source Facilities</p>
                            <div className="flex flex-col gap-1.5">
                              {referralReport.topSources.map(f => (
                                <div key={f.facilityName} className="flex items-center justify-between rounded-lg border bg-muted/40 px-3 py-2">
                                  <span className="text-sm">{f.facilityName}</span>
                                  <span className="text-sm text-muted-foreground">{f.referralCount}</span>
                                </div>
                              ))}
                            </div>
                          </div>
                        )}
                      </div>

                      <div>
                        <p className="mb-2 text-sm font-semibold">Recent Referral Activity</p>
                        {referralReport.recentActivity.length === 0 ? (
                          <p className="text-sm text-muted-foreground">No referral activity recorded yet.</p>
                        ) : (
                          <div className="flex flex-col gap-1.5">
                            {referralReport.recentActivity.map(r => {
                              const isOutgoing = r.fromFacility?.name === referralReport.facilityName;
                              return (
                                <div key={r.id} className="text-sm">
                                  <Badge variant={isOutgoing ? 'warning' : 'success'} className="mr-1.5">{isOutgoing ? '↗ Sent' : '↙ Received'}</Badge>
                                  <Badge variant={r.urgency === 'EMERGENCY' ? 'destructive' : r.urgency === 'URGENT' ? 'warning' : 'success'} className="mr-1.5">{r.urgency}</Badge>
                                  <span className="text-muted-foreground">
                                    {r.patient?.firstName} {r.patient?.lastName} {isOutgoing ? `to ${r.toFacility?.name}` : `from ${r.fromFacility?.name}`} — {r.reason} — {new Date(r.referredAt).toLocaleString()} — <span className="font-semibold text-foreground">{r.status}</span>
                                  </span>
                                </div>
                              );
                            })}
                          </div>
                        )}
                      </div>
                      </CardContent>
                    </Card>
                  )}

                  {prescriptionReport && (
                    <Card className="text-left">
                      <CardHeader>
                        <div className="flex flex-wrap items-start justify-between gap-2.5">
                          <CardTitle className="flex items-center gap-2 text-lg">
                            <Clipboard size={18} /> Facility Prescription Report
                          </CardTitle>
                          <div className="flex gap-2">
                            <Button variant="outline" size="sm" onClick={() => downloadReportCsv('/api/prescriptions/report/export', 'prescriptions.csv')}>
                              <Download size={14} /> Export CSV
                            </Button>
                            <Button variant="outline" size="sm" onClick={exportPrescriptionPdf}>
                              <FileText size={14} /> Export PDF
                            </Button>
                          </div>
                        </div>
                        <CardDescription>{prescriptionReport.facilityName}</CardDescription>
                      </CardHeader>
                      <CardContent>
                      <div className="mb-4 grid grid-cols-3 gap-2.5">
                        <div className="rounded-xl border bg-muted/40 p-3">
                          <p className="text-xs text-muted-foreground">Total Prescriptions ({reportRangeSuffix()})</p>
                          <p className="text-xl font-bold">{prescriptionReport.totalPrescriptions}</p>
                        </div>
                        <div className="rounded-xl border bg-muted/40 p-3">
                          <p className="text-xs text-muted-foreground">Active Prescriptions</p>
                          <p className="text-xl font-bold text-emerald-600">{prescriptionReport.activePrescriptions}</p>
                        </div>
                        <div className="rounded-xl border bg-muted/40 p-3">
                          <p className="text-xs text-muted-foreground">Issued Today</p>
                          <p className="text-xl font-bold text-sky-600">{prescriptionReport.issuedToday}</p>
                        </div>
                        <div className="rounded-xl border bg-muted/40 p-3">
                          <p className="text-xs text-muted-foreground">Unique Patients Prescribed</p>
                          <p className="text-xl font-bold">{prescriptionReport.uniquePatientsPrescribed}</p>
                        </div>
                      </div>

                      <div className="mb-4 grid grid-cols-2 gap-4">
                        {prescriptionReport.topMedications.length > 0 && (
                          <div>
                            <p className="mb-2 text-sm font-semibold">Top Prescribed Medications</p>
                            <div className="flex flex-col gap-1.5">
                              {prescriptionReport.topMedications.map(m => (
                                <div key={m.medicationName} className="flex items-center justify-between rounded-lg border bg-muted/40 px-3 py-2">
                                  <span className="text-sm">{m.medicationName}</span>
                                  <span className="text-sm text-muted-foreground">{m.prescriptionCount}</span>
                                </div>
                              ))}
                            </div>
                          </div>
                        )}
                        {prescriptionReport.topPrescribers.length > 0 && (
                          <div>
                            <p className="mb-2 text-sm font-semibold">Top Prescribers</p>
                            <div className="flex flex-col gap-1.5">
                              {prescriptionReport.topPrescribers.map(p => (
                                <div key={p.prescriberName} className="flex items-center justify-between rounded-lg border bg-muted/40 px-3 py-2">
                                  <span className="text-sm">Dr. {p.prescriberName}</span>
                                  <span className="text-sm text-muted-foreground">{p.prescriptionCount}</span>
                                </div>
                              ))}
                            </div>
                          </div>
                        )}
                      </div>

                      <div>
                        <p className="mb-2 text-sm font-semibold">Recent Prescriptions</p>
                        {prescriptionReport.recentPrescriptions.length === 0 ? (
                          <p className="text-sm text-muted-foreground">No prescriptions recorded yet.</p>
                        ) : (
                          <div className="flex flex-col gap-1.5">
                            {prescriptionReport.recentPrescriptions.map(p => (
                              <div key={p.id} className="text-sm">
                                {!p.active && <Badge variant="destructive" className="mr-1.5">INACTIVE</Badge>}
                                <span className="text-muted-foreground">
                                  <span className="text-foreground">{p.medication}</span> ({p.dosage}, {p.frequency}) for {p.patient?.firstName} {p.patient?.lastName} — {new Date(p.createdAt).toLocaleString()} by Dr. {p.doctor?.firstName} {p.doctor?.lastName}
                                </span>
                              </div>
                            ))}
                          </div>
                        )}
                      </div>
                      </CardContent>
                    </Card>
                  )}

                  {labResultReport && (
                    <Card className="text-left">
                      <CardHeader>
                        <div className="flex flex-wrap items-start justify-between gap-2.5">
                          <CardTitle className="flex items-center gap-2 text-lg">
                            <FileSpreadsheet size={18} /> Facility Lab Results Report
                          </CardTitle>
                          <div className="flex gap-2">
                            <Button variant="outline" size="sm" onClick={() => downloadReportCsv('/api/lab-results/report/export', 'lab-results.csv')}>
                              <Download size={14} /> Export CSV
                            </Button>
                            <Button variant="outline" size="sm" onClick={exportLabResultPdf}>
                              <FileText size={14} /> Export PDF
                            </Button>
                          </div>
                        </div>
                        <CardDescription>{labResultReport.facilityName}</CardDescription>
                      </CardHeader>
                      <CardContent>
                      <div className="mb-4 grid grid-cols-3 gap-2.5">
                        <div className="rounded-xl border bg-muted/40 p-3">
                          <p className="text-xs text-muted-foreground">Total Lab Results ({reportRangeSuffix()})</p>
                          <p className="text-xl font-bold">{labResultReport.totalLabResults}</p>
                        </div>
                        <div className="rounded-xl border bg-muted/40 p-3">
                          <p className="text-xs text-muted-foreground">Results Today</p>
                          <p className="text-xl font-bold text-sky-600">{labResultReport.resultsToday}</p>
                        </div>
                        <div className="rounded-xl border bg-muted/40 p-3">
                          <p className="text-xs text-muted-foreground">Unique Patients Tested</p>
                          <p className="text-xl font-bold">{labResultReport.uniquePatientsTested}</p>
                        </div>
                        <div className="rounded-xl border bg-muted/40 p-3">
                          <p className="text-xs text-muted-foreground">Unique Test Types</p>
                          <p className="text-xl font-bold">{labResultReport.uniqueTestTypes}</p>
                        </div>
                      </div>

                      <div className="mb-4 grid grid-cols-2 gap-4">
                        {labResultReport.topTestTypes.length > 0 && (
                          <div>
                            <p className="mb-2 text-sm font-semibold">Top Test Types</p>
                            <div className="flex flex-col gap-1.5">
                              {labResultReport.topTestTypes.map(t => (
                                <div key={t.testName} className="flex items-center justify-between rounded-lg border bg-muted/40 px-3 py-2">
                                  <span className="text-sm">{t.testName}</span>
                                  <span className="text-sm text-muted-foreground">{t.testCount}</span>
                                </div>
                              ))}
                            </div>
                          </div>
                        )}
                        {labResultReport.topOrderingStaff.length > 0 && (
                          <div>
                            <p className="mb-2 text-sm font-semibold">Top Ordering Staff</p>
                            <div className="flex flex-col gap-1.5">
                              {labResultReport.topOrderingStaff.map(s => (
                                <div key={s.staffName} className="flex items-center justify-between rounded-lg border bg-muted/40 px-3 py-2">
                                  <span className="text-sm">{s.staffName}</span>
                                  <span className="text-sm text-muted-foreground">{s.testCount}</span>
                                </div>
                              ))}
                            </div>
                          </div>
                        )}
                      </div>

                      <div>
                        <p className="mb-2 text-sm font-semibold">Recent Lab Results</p>
                        {labResultReport.recentResults.length === 0 ? (
                          <p className="text-sm text-muted-foreground">No lab results recorded yet.</p>
                        ) : (
                          <div className="flex flex-col gap-1.5">
                            {labResultReport.recentResults.map(r => (
                              <div key={r.id} className="text-sm text-muted-foreground">
                                <span className="text-foreground">{r.testName}</span>: {r.result}{r.unit ? ` ${r.unit}` : ''}{r.normalRange ? ` (normal: ${r.normalRange})` : ''} for {r.patient?.firstName} {r.patient?.lastName} — {new Date(r.testDate).toLocaleString()} by {r.staff?.firstName} {r.staff?.lastName}
                              </div>
                            ))}
                          </div>
                        )}
                      </div>
                      </CardContent>
                    </Card>
                  )}

                  <Card className="text-left">
                    <CardHeader>
                      <CardTitle className="flex items-center gap-2 text-base">
                        <User size={18} /> All Staff ({staffList.length})
                      </CardTitle>
                    </CardHeader>
                    <CardContent>
                    {staffList.length === 0 ? (
                      <p className="text-sm text-muted-foreground">No staff members found.</p>
                    ) : (
                      <div className="flex flex-col gap-2.5">
                        {staffList.map(s => (
                          <div
                            key={s.id}
                            className={cn('flex flex-wrap items-center justify-between gap-2.5 rounded-lg border p-3', !s.active && 'border-destructive/30 bg-destructive/5')}
                          >
                            <div>
                              <p className="text-sm font-semibold">
                                {s.firstName} {s.lastName} <span className="font-normal text-muted-foreground">({s.staffNumber})</span>
                              </p>
                              <p className="text-sm text-muted-foreground">
                                {s.role} | {s.facility?.name || 'No facility'} | {s.email}
                              </p>
                            </div>
                            <div className="flex items-center gap-2">
                              {s.active ? (
                                <>
                                  <Badge variant="success">Active</Badge>
                                  <Button variant="destructive" size="sm" onClick={() => handleDeactivateStaff(s.id)} disabled={loading}>
                                    Deactivate
                                  </Button>
                                </>
                              ) : (
                                <Badge variant="destructive">Inactive</Badge>
                              )}
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                    </CardContent>
                  </Card>

                  <Card className="text-left">
                    <CardHeader>
                      <CardTitle className="flex items-center gap-2 text-base">
                        <Building2 size={18} /> All Facilities ({facilitiesList.length})
                      </CardTitle>
                    </CardHeader>
                    <CardContent>
                    {facilitiesList.length === 0 ? (
                      <p className="text-sm text-muted-foreground">No facilities registered.</p>
                    ) : (
                      <div className="flex flex-col gap-2.5">
                        {facilitiesList.map(f => (
                          <div key={f.id} className="flex flex-wrap items-center justify-between gap-2.5 rounded-lg border bg-muted/40 p-3">
                            <div>
                              <p className="text-sm font-semibold">{f.name}</p>
                              <p className="text-sm text-muted-foreground">{f.province} | {f.address}</p>
                            </div>
                            <Badge variant={f.type === 'HOSPITAL' ? 'warning' : 'success'}>{f.type}</Badge>
                          </div>
                        ))}
                      </div>
                    )}
                    </CardContent>
                  </Card>
                </div>
              </div>
            )}
          </div>
        )}
      </main>

      {/* Footer */}
      <footer className="mt-auto border-t pt-8 pb-5 text-center">
        <p className="text-sm text-muted-foreground">
          &copy; {new Date().getFullYear()} Universal Digital Health Record System (UDHR). Authorized medical staff and patient access only.
        </p>
        <p className="mt-1 text-xs text-muted-foreground">
          System complies with the National Health Act and POPI Act of South Africa. Portals powered by Infermedica Triage & OpenFDA Databases.
        </p>
      </footer>
    </div>
    </div>
  );
}

export default App;
