import React, { useState, useEffect } from 'react';
import { 
  Activity, Heart, AlertTriangle, Shield, ShieldAlert, User, LogOut, Search, PlusCircle, 
  Calendar, MapPin, Phone, CheckCircle, XCircle, FileText, Pill, Compass, Clock, 
  Clipboard, RefreshCw, AlertCircle, FileSpreadsheet, Upload, Barcode
} from 'lucide-react';
import './App.css';

function App() {
  // Authentication State
  const [token, setToken] = useState(localStorage.getItem('token') || '');
  const [userRole, setUserRole] = useState(localStorage.getItem('role') || '');
  const [userIdNumber, setUserIdNumber] = useState(localStorage.getItem('idNumber') || '');
  const [userName, setUserName] = useState(localStorage.getItem('userName') || '');
  const [userFacilityId, setUserFacilityId] = useState(localStorage.getItem('facilityId') || '');
  
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
  const [showCatchUpForm, setShowCatchUpForm] = useState(false);
  const [catchUpForm, setCatchUpForm] = useState({
    vaccineName: '', doseNumber: '', scheduledDate: '', administeredDate: '', notes: ''
  });

  // Admin: Staff Management state
  const [staffList, setStaffList] = useState([]);
  const [facilitiesList, setFacilitiesList] = useState([]);
  const [staffRegForm, setStaffRegForm] = useState({
    staffNumber: '', firstName: '', lastName: '', role: 'NURSE', facilityId: '', email: '', password: ''
  });

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

      setToken(data.token);
      setUserRole(data.role);
      setUserIdNumber(data.idNumber || data.staffNumber);
      setUserName(data.fullName);
      setUserFacilityId(data.facilityId || '');
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
    localStorage.removeItem('facilityId');
    setToken('');
    setUserRole('');
    setUserIdNumber('');
    setUserName('');
    setUserFacilityId('');
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

  // Fetch data on login
  useEffect(() => {
    if (token) {
      if (userRole === 'PATIENT') {
        fetchPatientPortalData();
      } else {
        fetchClinicalAlerts();
      }
    }
  }, [token, userRole]);

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
        facilityId: facilitiesList.length > 0 ? String(facilitiesList[0].id) : '', email: '', password: ''
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
        email: patientRegForm.email
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
      return <span className="badge badge-red"><ShieldAlert size={14} />🔴 High (Go to Emergency)</span>;
    } else if (level === 'YELLOW') {
      return <span className="badge badge-yellow"><AlertTriangle size={14} />🟡 Moderate (Visit Clinic within 24h)</span>;
    } else {
      return <span className="badge badge-green"><CheckCircle size={14} />🟢 Low (Rest & Monitor at Home)</span>;
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

        {/* 1. Login Page */}
        {!token && (
          <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', flex: 1, padding: '20px 0' }}>
            <div className="glass-card" style={{ width: '450px', maxWidth: '100%', textAlign: 'center' }}>
              <Heart size={48} color="#4f46e5" style={{ margin: '0 auto 16px' }} />
              <h2 style={{ marginBottom: '8px' }}>Welcome to UDHR</h2>
              <p style={{ marginBottom: '24px' }}>Access your electronic health records, check symptoms, and review guidelines.</p>

              {/* Login Switcher Tabs */}
              <div style={{ display: 'flex', background: 'rgba(15, 23, 42, 0.6)', padding: '4px', borderRadius: '12px', marginBottom: '24px' }}>
                <button 
                  className="btn" 
                  style={{ flex: 1, background: loginRole === 'patient' ? 'var(--primary)' : 'transparent', color: '#fff', borderRadius: '10px', padding: '8px' }}
                  onClick={() => { setLoginRole('patient'); setErrorMessage(''); }}
                >
                  Patient Portal
                </button>
                <button 
                  className="btn" 
                  style={{ flex: 1, background: loginRole === 'staff' ? 'var(--primary)' : 'transparent', color: '#fff', borderRadius: '10px', padding: '8px' }}
                  onClick={() => { setLoginRole('staff'); setErrorMessage(''); }}
                >
                  Healthcare Staff
                </button>
              </div>

              {/* Login Forms */}
              <form onSubmit={handleLogin}>
                {loginRole === 'patient' ? (
                  <>
                    <div className="form-group">
                      <label htmlFor="patientId">South African ID Number</label>
                      <input 
                        type="text" 
                        id="patientId" 
                        value={patientIdNumber} 
                        onChange={(e) => setPatientIdNumber(e.target.value)} 
                        placeholder="e.g. 9001015000083" 
                        required 
                      />
                    </div>
                    <div className="form-group">
                      <label htmlFor="patientDob">Date of Birth</label>
                      <input 
                        type="date" 
                        id="patientDob" 
                        value={patientDob} 
                        onChange={(e) => setPatientDob(e.target.value)} 
                        required 
                      />
                    </div>
                    <div style={{ textAlign: 'left', marginBottom: '20px', background: 'rgba(79, 70, 229, 0.1)', padding: '12px', borderRadius: '8px', border: '1px solid rgba(79, 70, 229, 0.2)' }}>
                      <p style={{ fontSize: '0.8rem', color: '#a5b4fc' }}>
                        💡 <strong>Demo Patient Login:</strong> Use ID <code>9001015000083</code> and Date of Birth <code>1990-01-01</code> to view the pre-seeded patient (diabetic, hypertensive, penicillin allergic).
                      </p>
                    </div>
                  </>
                ) : (
                  <>
                    <div className="form-group">
                      <label htmlFor="staffNum">Staff Number</label>
                      <input 
                        type="text" 
                        id="staffNum" 
                        value={staffNumber} 
                        onChange={(e) => setStaffNumber(e.target.value)} 
                        placeholder="e.g. DOC001 or NUR001" 
                        required 
                      />
                    </div>
                    <div className="form-group">
                      <label htmlFor="staffPass">Password</label>
                      <input 
                        type="password" 
                        id="staffPass" 
                        value={staffPassword} 
                        onChange={(e) => setStaffPassword(e.target.value)} 
                        placeholder="••••••••" 
                        required 
                      />
                    </div>
                    <div style={{ textAlign: 'left', marginBottom: '20px', background: 'rgba(79, 70, 229, 0.1)', padding: '12px', borderRadius: '8px', border: '1px solid rgba(79, 70, 229, 0.2)' }}>
                      <p style={{ fontSize: '0.8rem', color: '#a5b4fc' }}>
                        💡 <strong>Demo Staff Logins:</strong><br />
                        - Doctor: <code>DOC001</code> / <code>Doctor@123</code><br />
                        - Nurse: <code>NUR001</code> / <code>Nurse@123</code>
                      </p>
                    </div>
                  </>
                )}

                <button type="submit" className="btn btn-primary" style={{ width: '100%' }} disabled={loading}>
                  {loading ? 'Logging in...' : 'Sign In'}
                </button>
              </form>
            </div>
          </div>
        )}

        {/* 2. Patient Portal View */}
        {token && userRole === 'PATIENT' && (
          <div className="dashboard-grid">
            
            {/* Sidebar Demographics Card */}
            <div className="dashboard-sidebar">
              <div className="glass-card" style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', borderBottom: '1px solid rgba(255,255,255,0.1)', paddingBottom: '16px' }}>
                  <div style={{ background: 'var(--primary)', width: '64px', height: '64px', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', fontSize: '1.8rem', fontWeight: 'bold', marginBottom: '12px', alignSelf: 'center' }}>
                    {patientProfile ? patientProfile.firstName.charAt(0) + patientProfile.lastName.charAt(0) : <User />}
                  </div>
                  <h3 style={{ color: '#fff' }}>{patientProfile?.firstName} {patientProfile?.lastName}</h3>
                  <p className="text-muted" style={{ fontSize: '0.85rem' }}>National Health ID: {patientProfile?.idNumber}</p>
                </div>
                
                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', fontSize: '0.9rem', textAlign: 'left' }}>
                  <div style={{ display: 'flex', gap: '10px' }}>
                    <Calendar size={16} className="text-muted" />
                    <div>
                      <p className="text-muted" style={{ fontSize: '0.75rem' }}>Date of Birth</p>
                      <p style={{ color: '#fff' }}>{patientProfile?.dateOfBirth}</p>
                    </div>
                  </div>
                  <div style={{ display: 'flex', gap: '10px' }}>
                    <Compass size={16} className="text-muted" />
                    <div>
                      <p className="text-muted" style={{ fontSize: '0.75rem' }}>Gender</p>
                      <p style={{ color: '#fff' }}>{patientProfile?.gender}</p>
                    </div>
                  </div>
                  <div style={{ display: 'flex', gap: '10px' }}>
                    <Phone size={16} className="text-muted" />
                    <div>
                      <p className="text-muted" style={{ fontSize: '0.75rem' }}>Contact Number</p>
                      <p style={{ color: '#fff' }}>{patientProfile?.contactNumber || 'Not provided'}</p>
                    </div>
                  </div>
                  <div style={{ display: 'flex', gap: '10px' }}>
                    <MapPin size={16} className="text-muted" />
                    <div>
                      <p className="text-muted" style={{ fontSize: '0.75rem' }}>Address</p>
                      <p style={{ color: '#fff', fontSize: '0.85rem' }}>{patientProfile?.address || 'Not provided'}</p>
                    </div>
                  </div>
                </div>
              </div>

              {/* Triage History List */}
              <div className="glass-card">
                <h3 style={{ marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px', fontSize: '1.2rem', color: '#fff' }}>
                  <Clock size={18} /> Symptom Check History
                </h3>
                {triageHistory.length === 0 ? (
                  <p className="text-muted" style={{ fontSize: '0.9rem' }}>No symptom checks completed yet.</p>
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', maxHeight: '350px', overflowY: 'auto' }}>
                    {triageHistory.map((check) => (
                      <div key={check.id} style={{ background: 'rgba(15, 23, 42, 0.4)', padding: '12px', borderRadius: '12px', border: '1px solid rgba(255,255,255,0.05)', textAlign: 'left' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                            {new Date(check.checkedAt).toLocaleDateString()}
                          </span>
                          <span style={{ 
                            fontSize: '0.7rem', 
                            fontWeight: 'bold', 
                            color: check.urgencyLevel === 'RED' ? 'var(--danger)' : check.urgencyLevel === 'YELLOW' ? 'var(--warning)' : 'var(--success)'
                          }}>
                            {check.urgencyLevel}
                          </span>
                        </div>
                        <p style={{ fontSize: '0.85rem', color: '#fff', margin: 0 }}>{check.recommendation.split('[')[0]}</p>
                        {check.details && check.details.length > 0 && (
                          <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap', marginTop: '8px' }}>
                            {check.details.map((d, idx) => (
                              <span key={idx} style={{ background: 'rgba(255, 255, 255, 0.05)', color: '#cbd5e1', border: '1px solid rgba(255,255,255,0.08)', padding: '2px 8px', borderRadius: '4px', fontSize: '0.7rem' }}>
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

            {/* Main Portal Panels */}
            <div className="dashboard-main">
              
              {/* Dynamic Health Guidance Banner */}
              <div className="glass-card" style={{ borderLeft: '4px solid var(--primary)', background: 'linear-gradient(90deg, rgba(79, 70, 229, 0.1) 0%, rgba(14, 165, 233, 0.05) 100%)', textAlign: 'left' }}>
                <h2 style={{ fontSize: '1.5rem', marginBottom: '8px', color: '#fff' }}>Personalized Health Guidance Portal</h2>
                <p style={{ fontSize: '0.95rem', marginBottom: '16px' }}>
                  Based on your active conditions, we have compiled specialized dietary guidelines and safety warnings.
                </p>
                
                {/* Active Tags */}
                <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                  {healthGuidance?.conditions.map((c, i) => (
                    <span key={i} style={{ background: 'rgba(79, 70, 229, 0.2)', color: '#a5b4fc', border: '1px solid rgba(79, 70, 229, 0.3)', padding: '4px 12px', borderRadius: '6px', fontSize: '0.8rem', fontWeight: 600 }}>
                      Condition: {c}
                    </span>
                  ))}
                  {healthGuidance?.allergies.map((a, i) => (
                    <span key={i} style={{ background: 'rgba(239, 68, 68, 0.15)', color: '#fca5a5', border: '1px solid rgba(239, 68, 68, 0.3)', padding: '4px 12px', borderRadius: '6px', fontSize: '0.8rem', fontWeight: 600 }}>
                      ⚠️ Allergy: {a}
                    </span>
                  ))}
                </div>
              </div>

              {/* Doctor Clinical Warnings Panel */}
              {patientAlerts && patientAlerts.length > 0 && (
                <div className="glass-card" style={{ borderLeft: '4px solid #f59e0b', background: 'rgba(245, 158, 11, 0.05)', textAlign: 'left' }}>
                  <h3 style={{ color: '#fff', fontSize: '1.2rem', marginBottom: '12px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <ShieldAlert color="#f59e0b" /> Clinical Warnings & Doctor's Instructions
                  </h3>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    {patientAlerts.map((alert) => (
                      <div key={alert.id} style={{ background: 'rgba(15, 23, 42, 0.4)', padding: '12px', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.03)' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                          <span style={{ fontSize: '0.8rem', fontWeight: 'bold', color: alert.severity === 'CRITICAL' ? '#ef4444' : '#f59e0b' }}>
                            {alert.severity} WARNING
                          </span>
                          <span className="text-muted" style={{ fontSize: '0.75rem' }}>
                            {new Date(alert.createdAt).toLocaleDateString()}
                          </span>
                        </div>
                        <p style={{ color: '#fff', fontSize: '0.85rem', lineHeight: '1.4' }}>{alert.message}</p>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Drug-Food Conflict Audit Panel (Patient dashboard warning) */}
              {drugFoodConflicts && drugFoodConflicts.length > 0 && (
                <div className="glass-card" style={{ borderLeft: '4px solid #ef4444', background: 'rgba(239, 68, 68, 0.05)', textAlign: 'left' }}>
                  <h3 style={{ color: '#fff', fontSize: '1.2rem', marginBottom: '12px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <AlertTriangle color="#ef4444" /> Active Drug-Food Interactions Flagged
                  </h3>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    {drugFoodConflicts.map((c, i) => (
                      <div key={i} style={{ background: 'rgba(15, 23, 42, 0.4)', padding: '12px', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.03)' }}>
                        <p style={{ color: '#fff', fontSize: '0.85rem' }}><strong>Medication:</strong> {c.medication} | <strong>Ingredient:</strong> {c.ingredient}</p>
                        <p style={{ fontSize: '0.8rem', color: c.severity === 'CRITICAL' ? '#fca5a5' : '#fde047', marginTop: '4px', lineHeight: 1.4 }}>{c.message}</p>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Medication Reminders & Adherence Widget */}
              <div className="glass-card" style={{ textAlign: 'left' }}>
                <h2 style={{ fontSize: '1.4rem', color: '#fff', marginBottom: '12px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <AlertCircle className="text-secondary" style={{ color: '#f59e0b' }} /> 🔔 Medication Reminders & Adherence
                </h2>
                
                {/* Adherence Compliance Widget */}
                {reminderData?.stats && reminderData.stats.totalDoses > 0 && (
                  <div style={{ background: 'rgba(15, 23, 42, 0.5)', padding: '16px', borderRadius: '12px', border: '1px solid rgba(255,255,255,0.05)', marginBottom: '20px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px', alignItems: 'center' }}>
                      <span style={{ fontSize: '0.9rem', color: '#fff', fontWeight: 600 }}>Weekly Adherence Score</span>
                      <span style={{ fontSize: '1.1rem', fontWeight: 'bold', color: reminderData.stats.adherenceScore >= 80 ? 'var(--success)' : reminderData.stats.adherenceScore >= 50 ? 'var(--warning)' : 'var(--danger)' }}>
                        {reminderData.stats.adherenceScore}%
                      </span>
                    </div>
                    <div style={{ background: 'rgba(255,255,255,0.1)', height: '10px', borderRadius: '5px', overflow: 'hidden', marginBottom: '8px' }}>
                      <div style={{ background: 'linear-gradient(90deg, #10b981 0%, #3b82f6 100%)', width: `${reminderData.stats.adherenceScore}%`, height: '100%' }}></div>
                    </div>
                    <p className="text-muted" style={{ fontSize: '0.8rem' }}>
                      You have taken {reminderData.stats.takenDoses} out of {reminderData.stats.totalDoses} scheduled doses this week. Keep it up!
                    </p>
                  </div>
                )}

                {/* Today's Reminders List */}
                {!reminderData || reminderData.adherenceLogs.length === 0 ? (
                  <p className="text-muted" style={{ fontSize: '0.95rem' }}>No medication reminders scheduled for today.</p>
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                    {reminderData.adherenceLogs.map((log) => {
                      const timeString = new Date(log.scheduledTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
                      return (
                        <div 
                          key={log.id} 
                          style={{ 
                            display: 'flex', 
                            justifyContent: 'space-between', 
                            alignItems: 'center', 
                            padding: '16px', 
                            borderRadius: '12px', 
                            background: log.status === 'TAKEN' ? 'rgba(16, 185, 129, 0.08)' : log.status === 'MISSED' ? 'rgba(239, 68, 68, 0.08)' : 'rgba(15, 23, 42, 0.4)',
                            border: `1px solid ${log.status === 'TAKEN' ? 'rgba(16, 185, 129, 0.2)' : log.status === 'MISSED' ? 'rgba(239, 68, 68, 0.2)' : 'rgba(255,255,255,0.05)'}`
                          }}
                        >
                          <div>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                              <span style={{ fontSize: '1rem', fontWeight: 600, color: '#fff' }}>{log.reminder.prescription.medication}</span>
                              <span className="text-muted" style={{ fontSize: '0.8rem' }}>({log.reminder.prescription.dosage})</span>
                            </div>
                            <p className="text-muted" style={{ fontSize: '0.75rem', marginTop: '4px' }}>
                              Scheduled Time: <strong style={{ color: '#fff' }}>{timeString}</strong> | Frequency: {log.reminder.frequency}
                            </p>
                            {log.takenAt && (
                              <p className="text-muted" style={{ fontSize: '0.7rem', color: '#a7f3d0', marginTop: '2px' }}>
                                Taken at: {new Date(log.takenAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                              </p>
                            )}
                            {log.status === 'PENDING' ? (
                              <input 
                                type="text"
                                placeholder="How does this make you feel? (e.g. side effects, dizzy...)"
                                value={adherenceNotes[log.id] || ''}
                                onChange={(e) => setAdherenceNotes({ ...adherenceNotes, [log.id]: e.target.value })}
                                style={{
                                  marginTop: '8px',
                                  width: '100%',
                                  padding: '6px 10px',
                                  fontSize: '0.8rem',
                                  borderRadius: '6px',
                                  background: 'rgba(255, 255, 255, 0.05)',
                                  border: '1px solid rgba(255, 255, 255, 0.1)',
                                  color: '#fff'
                                }}
                              />
                            ) : (
                              log.notes && (
                                <p className="text-muted" style={{ fontSize: '0.75rem', marginTop: '6px', fontStyle: 'italic', color: '#fca5a5' }}>
                                  Patient feedback: "{log.notes}"
                                </p>
                              )
                            )}
                          </div>

                          <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                            {log.status === 'PENDING' ? (
                              <>
                                <button className="btn btn-success" onClick={() => handleUpdateAdherence(log.id, 'TAKEN', adherenceNotes[log.id] || '')} style={{ padding: '6px 12px', fontSize: '0.8rem', display: 'flex', alignItems: 'center', gap: '4px' }}>
                                  <CheckCircle size={14} /> Taken
                                </button>
                                <button className="btn btn-danger" onClick={() => handleUpdateAdherence(log.id, 'MISSED', adherenceNotes[log.id] || '')} style={{ padding: '6px 12px', fontSize: '0.8rem', display: 'flex', alignItems: 'center', gap: '4px' }}>
                                  <XCircle size={14} /> Missed
                                </button>
                              </>
                            ) : (
                              <span className={`badge ${log.status === 'TAKEN' ? 'badge-green' : 'badge-red'}`}>
                                {log.status}
                              </span>
                            )}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>

              {/* Interactive Symptom Checker */}
              <div className="glass-card" style={{ textAlign: 'left' }}>
                <h2 style={{ fontSize: '1.4rem', color: '#fff', marginBottom: '12px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <Heart className="text-secondary" /> Symptom Checker & Care Navigator
                </h2>
                <p className="text-muted" style={{ marginBottom: '20px', fontSize: '0.9rem' }}>
                  Select the symptoms you are currently experiencing. Our care navigation engine (powered by Infermedica) will recommend the appropriate urgency level. <em>Note: This is not a diagnosis.</em>
                </p>

                {/* Symptom Checkbox Grid */}
                <div className="grid grid-cols-3" style={{ gap: '12px', marginBottom: '24px' }}>
                  {symptomsList.map((symptom) => {
                    const isSelected = selectedSymptoms.includes(symptom.id);
                    return (
                      <div 
                        key={symptom.id} 
                        className="flex items-center gap-4"
                        onClick={() => handleSymptomToggle(symptom.id)}
                        style={{ 
                          padding: '12px', 
                          background: isSelected ? 'rgba(79, 70, 229, 0.25)' : 'rgba(15, 23, 42, 0.4)', 
                          border: isSelected ? '1px solid var(--primary)' : '1px solid rgba(255,255,255,0.05)',
                          borderRadius: '12px',
                          cursor: 'pointer',
                          transition: 'all 0.2s ease'
                        }}
                      >
                        <input 
                          type="checkbox" 
                          checked={isSelected}
                          onChange={() => {}} // Handled by div onClick
                          style={{ width: '18px', height: '18px', cursor: 'pointer', pointerEvents: 'none' }}
                        />
                        <div>
                          <p style={{ color: '#fff', fontSize: '0.9rem', fontWeight: 500 }}>{symptom.name}</p>
                          <p className="text-muted" style={{ fontSize: '0.75rem' }}>ICD-10: {symptom.icd10Code || 'N/A'}</p>
                        </div>
                      </div>
                    );
                  })}
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
                  {selectedSymptoms.length > 0 && (
                    <button className="btn btn-secondary" onClick={() => setSelectedSymptoms([])}>
                      Clear Selection
                    </button>
                  )}
                  <button className="btn btn-primary" onClick={handleSymptomCheckSubmit} disabled={selectedSymptoms.length === 0 || loading}>
                    {loading ? 'Analyzing...' : `Analyze ${selectedSymptoms.length} Symptom(s)`}
                  </button>
                </div>

                {/* Triage Recommendation Output Modal */}
                {triageResult && (
                   <div style={{
                     position: 'fixed',
                     top: 0,
                     left: 0,
                     width: '100%',
                     height: '100%',
                     background: 'rgba(15,23,42,0.85)',
                     display: 'flex',
                     justifyContent: 'center',
                     alignItems: 'center',
                     zIndex: 9999,
                     backdropFilter: 'blur(8px)',
                     padding: '20px'
                   }}>
                     <div className="glass-card" style={{ 
                       maxWidth: '550px', 
                       width: '100%', 
                       border: '1px solid rgba(255,255,255,0.1)', 
                       background: '#0f172a',
                       padding: '24px',
                       borderRadius: '16px',
                       boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.5), 0 10px 10px -5px rgba(0, 0, 0, 0.04)',
                       textAlign: 'left'
                     }}>
                       <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid rgba(255,255,255,0.1)', paddingBottom: '12px', marginBottom: '16px' }}>
                         <h4 style={{ color: '#fff', fontSize: '1.25rem', fontWeight: 'bold' }}>Triage Recommendation</h4>
                         {getUrgencyBadge(triageResult.urgencyLevel)}
                       </div>
                       <p style={{ color: '#fff', fontSize: '1.05rem', fontWeight: 500, lineHeight: 1.6, marginBottom: '16px' }}>
                         {triageResult.recommendation.split('[')[0]}
                       </p>
                       <p className="text-muted" style={{ fontSize: '0.75rem', marginBottom: '24px', borderTop: '1px solid rgba(255,255,255,0.05)', paddingTop: '12px' }}>
                         ⚠️ <strong>Disclaimer:</strong> This tool only provides care recommendations based on symptoms. It does not replace professional medical evaluation. If you feel extremely unwell, seek medical help immediately.
                       </p>
                       <button 
                         className="btn btn-primary" 
                         onClick={() => setTriageResult(null)} 
                         style={{ width: '100%', padding: '10px' }}
                       >
                         Acknowledge & Close
                       </button>
                     </div>
                   </div>
                 )}
              </div>

              {/* Feature 3: Interactive Food Ingredient Checker */}
              <div className="glass-card" style={{ textAlign: 'left' }}>
                <h2 style={{ fontSize: '1.4rem', color: '#fff', marginBottom: '12px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <FileSpreadsheet className="text-secondary" style={{ color: '#0ea5e9' }} /> Food Ingredient Checker & Safety Scanner
                </h2>
                <p className="text-muted" style={{ marginBottom: '20px', fontSize: '0.9rem' }}>
                  Input ingredient lists manually, query items via Open Food Facts, or scan labels from packaging photographs (OCR) to evaluate their safety against your medical records.
                </p>

                {/* Input Method Selector */}
                <div style={{ display: 'flex', background: 'rgba(15, 23, 42, 0.6)', padding: '4px', borderRadius: '12px', marginBottom: '20px', maxWidth: '500px' }}>
                  <button 
                    className="btn" 
                    style={{ flex: 1, background: foodInputMethod === 'type' ? 'var(--primary)' : 'transparent', color: '#fff', borderRadius: '10px', padding: '6px 12px', fontSize: '0.85rem' }}
                    onClick={() => setFoodInputMethod('type')}
                  >
                    Type Ingredients
                  </button>
                  <button 
                    className="btn" 
                    style={{ flex: 1, background: foodInputMethod === 'search' ? 'var(--primary)' : 'transparent', color: '#fff', borderRadius: '10px', padding: '6px 12px', fontSize: '0.85rem' }}
                    onClick={() => setFoodInputMethod('search')}
                  >
                    Open Food Facts
                  </button>
                  <button 
                    className="btn" 
                    style={{ flex: 1, background: foodInputMethod === 'upload' ? 'var(--primary)' : 'transparent', color: '#fff', borderRadius: '10px', padding: '6px 12px', fontSize: '0.85rem' }}
                    onClick={() => setFoodInputMethod('upload')}
                  >
                    Upload Label (OCR)
                  </button>
                </div>

                {/* Dynamic Inputs based on Selector */}
                {foodInputMethod === 'type' && (
                  <div className="form-group">
                    <label>Ingredients List (separate with commas)</label>
                    <textarea 
                      value={ingredientsInput} 
                      onChange={(e) => setIngredientsInput(e.target.value)} 
                      placeholder="e.g. Sugar, Wheat Flour, Sodium Chloride, Peanut Butter, Vegetable Fat, Milk..." 
                      rows={3}
                    />
                  </div>
                )}

                {foodInputMethod === 'search' && (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', marginBottom: '20px' }}>
                    <div style={{ display: 'flex', gap: '12px' }}>
                      <div style={{ width: '150px' }}>
                        <label>Lookup Type</label>
                        <select value={lookupType} onChange={(e) => setLookupType(e.target.value)} style={{ marginTop: '8px' }}>
                          <option value="barcode">Barcode</option>
                          <option value="search">Product Name</option>
                        </select>
                      </div>
                      <div style={{ flex: 1 }}>
                        <label>{lookupType === 'barcode' ? 'Product Barcode' : 'Search Terms'}</label>
                        <div style={{ display: 'flex', gap: '8px', marginTop: '8px' }}>
                          <input 
                            type="text" 
                            value={productQuery} 
                            onChange={(e) => setProductQuery(e.target.value)} 
                            placeholder={lookupType === 'barcode' ? 'e.g. 737628064502' : 'e.g. wheat bread'} 
                          />
                          <button className="btn btn-secondary" onClick={handleProductLookup} disabled={loading} style={{ whiteSpace: 'nowrap' }}>
                            {lookupType === 'barcode' ? <Barcode size={18} /> : <Search size={18} />} Fetch
                          </button>
                        </div>
                      </div>
                    </div>
                    {ingredientsInput && (
                      <div className="form-group">
                        <label>Fetched Ingredients</label>
                        <textarea value={ingredientsInput} onChange={(e) => setIngredientsInput(e.target.value)} rows={2} />
                      </div>
                    )}
                  </div>
                )}

                {foodInputMethod === 'upload' && (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', marginBottom: '20px' }}>
                    <label>Upload Food Label Photo</label>
                    <div style={{ border: '2px dashed var(--card-border)', borderRadius: '12px', padding: '24px', textAlign: 'center', background: 'rgba(15, 23, 42, 0.4)', position: 'relative', cursor: 'pointer' }}>
                      <Upload size={32} className="text-muted" style={{ margin: '0 auto 8px' }} />
                      <p style={{ fontSize: '0.9rem', color: 'var(--text-muted)' }}>Choose label file or drag it here</p>
                      <p style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '4px' }}>PNG, JPG or JPEG. Max size 5MB.</p>
                      <input 
                        type="file" 
                        accept="image/*"
                        onChange={handleOcrUpload}
                        style={{ position: 'absolute', top: 0, left: 0, width: '100%', height: '100%', opacity: 0, cursor: 'pointer' }}
                      />
                    </div>
                    {ingredientsInput && (
                      <div className="form-group">
                        <label>Extracted Ingredients (OCR Text)</label>
                        <textarea value={ingredientsInput} onChange={(e) => setIngredientsInput(e.target.value)} rows={2} />
                      </div>
                    )}
                    <div style={{ background: 'rgba(14, 165, 233, 0.1)', padding: '12px', borderRadius: '8px', border: '1px solid rgba(14, 165, 233, 0.2)' }}>
                      <p style={{ fontSize: '0.8rem', color: '#7dd3fc' }}>
                        💡 <strong>OCR Demo Trigger:</strong> Select any file. If the file name contains <code>juice</code>, <code>chips</code>, or <code>bread</code>, it will automatically extract matching condition-specific ingredients!
                      </p>
                    </div>
                  </div>
                )}

                {ingredientsInput && (
                  <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '16px' }}>
                    <button className="btn btn-primary" onClick={handleCheckIngredients} disabled={loading}>
                      {loading ? 'Analyzing...' : 'Analyze Safety Profiles'}
                    </button>
                  </div>
                )}

                {/* Analysis Results Display */}
                {checkResults.length > 0 && (
                  <div style={{ marginTop: '24px', borderTop: '1px solid rgba(255,255,255,0.08)', paddingTop: '20px' }}>
                    <h4 style={{ color: '#fff', fontSize: '1.1rem', marginBottom: '16px' }}>Scanned Ingredients Analysis</h4>
                    
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                      {checkResults.map((res, i) => (
                        <div 
                          key={i} 
                          style={{ 
                            display: 'flex', 
                            justifyContent: 'space-between', 
                            alignItems: 'center', 
                            padding: '12px 16px', 
                            borderRadius: '12px',
                            background: res.status === 'DANGER' ? 'rgba(239, 68, 68, 0.08)' : res.status === 'CAUTION' ? 'rgba(245, 158, 11, 0.08)' : 'rgba(16, 185, 129, 0.08)',
                            border: `1px solid ${res.status === 'DANGER' ? 'rgba(239, 68, 68, 0.2)' : res.status === 'CAUTION' ? 'rgba(245, 158, 11, 0.2)' : 'rgba(16, 185, 129, 0.2)'}`
                          }}
                        >
                          <div>
                            <span style={{ fontWeight: 600, color: '#fff', fontSize: '0.95rem' }}>{res.name}</span>
                            <p style={{ 
                              fontSize: '0.8rem', 
                              color: res.status === 'DANGER' ? '#fca5a5' : res.status === 'CAUTION' ? '#fde047' : '#a7f3d0',
                              marginTop: '2px' 
                            }}>
                              {res.reason}
                            </p>
                          </div>
                          
                          <span className={`badge ${res.status === 'DANGER' ? 'badge-red' : res.status === 'CAUTION' ? 'badge-yellow' : 'badge-green'}`}>
                            {res.status}
                          </span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>

              {/* Health Guidance Details */}
              <div className="grid grid-cols-2">
                
                {/* Dietary Guidelines Panel */}
                <div className="glass-card" style={{ textAlign: 'left' }}>
                  <h3 style={{ fontSize: '1.25rem', color: '#fff', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <Compass style={{ color: '#10b981' }} /> Personal Dietary Guidelines
                  </h3>
                  
                  {healthGuidance?.dietaryGuidelines.length === 0 ? (
                    <p className="text-muted" style={{ fontSize: '0.9rem' }}>No dietary guidelines matching your conditions.</p>
                  ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                      {/* Foods to Eat */}
                      <div>
                        <h4 style={{ fontSize: '0.9rem', color: 'var(--success)', display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '8px' }}>
                          <CheckCircle size={16} /> Recommended Foods to Eat
                        </h4>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                          {healthGuidance?.dietaryGuidelines.filter(g => g.foodType === 'EAT').map((item) => (
                            <div key={item.id} style={{ background: 'rgba(16, 185, 129, 0.05)', padding: '10px 12px', borderRadius: '8px', border: '1px solid rgba(16, 185, 129, 0.1)' }}>
                              <p style={{ color: '#fff', fontSize: '0.9rem', fontWeight: 600 }}>{item.foodItem}</p>
                              <p className="text-muted" style={{ fontSize: '0.8rem' }}>{item.description}</p>
                              <span style={{ fontSize: '0.65rem', color: 'var(--text-muted)' }}>Source: {item.source} (ICD-10 Aligned)</span>
                            </div>
                          ))}
                        </div>
                      </div>

                      {/* Foods to Avoid */}
                      <div>
                        <h4 style={{ fontSize: '0.9rem', color: 'var(--danger)', display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '8px' }}>
                          <XCircle size={16} /> Foods to Strict Limit / Avoid
                        </h4>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                          {healthGuidance?.dietaryGuidelines.filter(g => g.foodType === 'AVOID').map((item) => (
                            <div key={item.id} style={{ background: 'rgba(239, 68, 68, 0.05)', padding: '10px 12px', borderRadius: '8px', border: '1px solid rgba(239, 68, 68, 0.1)' }}>
                              <p style={{ color: '#fff', fontSize: '0.9rem', fontWeight: 600 }}>{item.foodItem}</p>
                              <p className="text-muted" style={{ fontSize: '0.8rem' }}>{item.description}</p>
                              <span style={{ fontSize: '0.65rem', color: 'var(--text-muted)' }}>Source: {item.source} (ICD-10 Aligned)</span>
                            </div>
                          ))}
                        </div>
                      </div>
                    </div>
                  )}
                </div>

                {/* Health & Lifestyle Tips Panel */}
                <div className="glass-card" style={{ textAlign: 'left' }}>
                  <h3 style={{ fontSize: '1.25rem', color: '#fff', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <Clipboard style={{ color: '#f59e0b' }} /> Lifestyle & Management Tips
                  </h3>
                  
                  {healthGuidance?.healthTips.length === 0 ? (
                    <p className="text-muted" style={{ fontSize: '0.9rem' }}>No custom health tips available.</p>
                  ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                      {healthGuidance?.healthTips.map((tip) => (
                        <div key={tip.id} style={{ background: 'rgba(15, 23, 42, 0.4)', padding: '12px', borderRadius: '12px', border: '1px solid rgba(255,255,255,0.05)' }}>
                          <span style={{ fontSize: '0.7rem', color: 'var(--warning)', fontWeight: 600, background: 'rgba(245, 158, 11, 0.1)', padding: '2px 8px', borderRadius: '4px', float: 'right' }}>
                            {tip.tipType}
                          </span>
                          <h4 style={{ color: '#fff', fontSize: '0.95rem', fontWeight: 600, marginBottom: '6px' }}>{tip.title}</h4>
                          <p className="text-muted" style={{ fontSize: '0.85rem', marginBottom: '6px' }}>{tip.description}</p>
                          <span style={{ fontSize: '0.65rem', color: 'var(--text-muted)' }}>Source: {tip.source} (SA Dept of Health)</span>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>

              {/* FDA Medication Warnings Panel */}
              <div className="glass-card" style={{ textAlign: 'left' }}>
                <h3 style={{ fontSize: '1.25rem', color: '#fff', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <ShieldAlert className="text-danger" style={{ color: '#ef4444' }} /> Medication Allergy & OpenFDA Safety Warnings
                </h3>

                {healthGuidance?.medicationWarnings && Object.keys(healthGuidance.medicationWarnings).length === 0 ? (
                  <p className="text-muted" style={{ fontSize: '0.9rem' }}>No medication allergies registered.</p>
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
                    {healthGuidance?.medicationWarnings && Object.entries(healthGuidance.medicationWarnings).map(([allergen, warnings]) => (
                      <div key={allergen} style={{ border: '1px solid rgba(239, 68, 68, 0.2)', borderRadius: '12px', padding: '16px', background: 'rgba(239, 68, 68, 0.02)' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '12px' }}>
                          <span style={{ background: '#ef4444', color: '#fff', padding: '4px 10px', borderRadius: '6px', fontSize: '0.8rem', fontWeight: 'bold' }}>
                            ALLERGEN: {allergen.toUpperCase()}
                          </span>
                          <span className="text-muted" style={{ fontSize: '0.85rem' }}>Medication cross-reactivity and warnings from OpenFDA:</span>
                        </div>

                        {warnings.length === 0 ? (
                          <p className="text-muted" style={{ fontSize: '0.85rem' }}>No FDA alerts found for this allergen. Consult your doctor.</p>
                        ) : (
                          <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '12px' }}>
                            {warnings.map((w, idx) => (
                              <div key={idx} style={{ background: 'rgba(15, 23, 42, 0.5)', padding: '12px', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.03)' }}>
                                <p style={{ color: '#fff', fontSize: '0.9rem', fontWeight: 600 }}>
                                  ⚠️ Avoid: <span style={{ color: '#fca5a5' }}>{w.genericName}</span> ({w.brandName})
                                </p>
                                <p className="text-muted" style={{ fontSize: '0.8rem', marginTop: '4px', lineHeight: 1.4, background: 'rgba(0,0,0,0.2)', padding: '8px', borderRadius: '6px', fontStyle: 'italic' }}>
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
              </div>
            </div>
          </div>
        )}

        {/* 3. Healthcare Staff View */}
        {token && userRole !== 'PATIENT' && (
          <div>
            {/* Tab Switcher for Staff */}
            <div style={{ display: 'flex', background: 'rgba(15, 23, 42, 0.6)', padding: '4px', borderRadius: '12px', marginBottom: '24px', maxWidth: '600px', margin: '0 auto' }}>
              <button 
                className="btn" 
                style={{ flex: 1, background: activeTabStaff === 'patients' ? 'var(--primary)' : 'transparent', color: '#fff', borderRadius: '10px', padding: '10px', fontSize: '0.9rem' }}
                onClick={() => setActiveTabStaff('patients')}
              >
                Locate & Manage Patients
              </button>
              <button 
                className="btn" 
                style={{ 
                  flex: 1, 
                  background: activeTabStaff === 'alerts' ? 'var(--primary)' : 'transparent', 
                  color: '#fff', 
                  borderRadius: '10px', 
                  padding: '10px', 
                  fontSize: '0.9rem',
                  display: 'flex', 
                  alignItems: 'center', 
                  justifyContent: 'center', 
                  gap: '6px'
                }}
                onClick={() => { setActiveTabStaff('alerts'); fetchClinicalAlerts(); }}
              >
                🚨 Clinical Alerts {clinicalAlerts && clinicalAlerts.length > 0 && (
                  <span style={{ background: '#ef4444', color: '#fff', fontSize: '0.75rem', padding: '2px 8px', borderRadius: '10px', fontWeight: 'bold' }}>
                    {clinicalAlerts.length}
                  </span>
                )}
              </button>
              {userRole === 'ADMIN' && (
                <button
                  className="btn"
                  style={{ flex: 1, background: activeTabStaff === 'staff' ? 'var(--primary)' : 'transparent', color: '#fff', borderRadius: '10px', padding: '10px', fontSize: '0.9rem' }}
                  onClick={() => { setActiveTabStaff('staff'); fetchStaffList(); fetchFacilitiesList(); }}
                >
                  👥 Staff Management
                </button>
              )}
            </div>

            {activeTabStaff === 'patients' ? (
              <div className="dashboard-grid">
                
                {/* Search Patient & Register Panel */}
                <div className="dashboard-sidebar">
                  
                  {/* Search Card */}
                  <div className="glass-card" style={{ textAlign: 'left' }}>
                    <h3 style={{ color: '#fff', fontSize: '1.2rem', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <Search size={18} /> Locate Patient File
                    </h3>
                    <form onSubmit={handleSearchPatient}>
                      <div className="form-group" style={{ marginBottom: '12px' }}>
                        <label htmlFor="searchId">ID Number or File Number (UHID)</label>
                        <input
                          type="text"
                          id="searchId"
                          value={searchId}
                          onChange={(e) => setSearchId(e.target.value)}
                          placeholder="ID number, or UDHR-... file number for a newborn"
                          required
                        />
                      </div>
                      <button type="submit" className="btn btn-primary" style={{ width: '100%' }} disabled={loading}>
                        <Search size={16} /> {loading ? 'Searching...' : 'Search Record'}
                      </button>
                    </form>
                  </div>

                  {/* Quick Register Card */}
                  <div className="glass-card" style={{ textAlign: 'left' }}>
                    <h3 style={{ color: '#fff', fontSize: '1.2rem', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <PlusCircle size={18} /> Register Patient
                    </h3>
                    <form onSubmit={handleRegisterPatient}>
                      <div
                        style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '16px', background: 'rgba(14, 165, 233, 0.08)', border: '1px solid rgba(14, 165, 233, 0.2)', borderRadius: '10px', padding: '10px 12px', cursor: 'pointer' }}
                        onClick={() => setIsNewbornMode(!isNewbornMode)}
                      >
                        <input type="checkbox" checked={isNewbornMode} onChange={() => {}} style={{ width: '16px', height: '16px', pointerEvents: 'none' }} />
                        <div>
                          <p style={{ color: '#fff', fontSize: '0.85rem', fontWeight: 600, margin: 0 }}>Register a newborn</p>
                          <p className="text-muted" style={{ fontSize: '0.75rem', margin: 0 }}>No ID number needed yet — opens a file from birth and starts the EPI vaccine schedule.</p>
                        </div>
                      </div>
                      <div className="form-group">
                        <label>{isNewbornMode ? 'ID Number (leave blank — not yet issued)' : 'ID Number'}</label>
                        <input
                          type="text"
                          value={patientRegForm.idNumber}
                          onChange={(e) => setPatientRegForm({...patientRegForm, idNumber: e.target.value})}
                          required={!isNewbornMode}
                          placeholder={isNewbornMode ? 'Leave blank if not yet registered with Home Affairs' : 'SA ID number'}
                        />
                      </div>
                      <div className="form-group">
                        <label>First Name</label>
                        <input 
                          type="text" 
                          value={patientRegForm.firstName} 
                          onChange={(e) => setPatientRegForm({...patientRegForm, firstName: e.target.value})} 
                          required 
                        />
                      </div>
                      <div className="form-group">
                        <label>Last Name</label>
                        <input 
                          type="text" 
                          value={patientRegForm.lastName} 
                          onChange={(e) => setPatientRegForm({...patientRegForm, lastName: e.target.value})} 
                          required 
                        />
                      </div>
                      <div className="form-group">
                        <label>Date of Birth</label>
                        <input 
                          type="date" 
                          value={patientRegForm.dateOfBirth} 
                          onChange={(e) => setPatientRegForm({...patientRegForm, dateOfBirth: e.target.value})} 
                          required 
                        />
                      </div>
                      <div className="form-group">
                        <label>Gender</label>
                        <select 
                          value={patientRegForm.gender} 
                          onChange={(e) => setPatientRegForm({...patientRegForm, gender: e.target.value})}
                        >
                          <option value="MALE">Male</option>
                          <option value="FEMALE">Female</option>
                          <option value="OTHER">Other</option>
                        </select>
                      </div>
                      <div className="form-group">
                        <label>Contact Number</label>
                        <input 
                          type="text" 
                          value={patientRegForm.contactNumber} 
                          onChange={(e) => setPatientRegForm({...patientRegForm, contactNumber: e.target.value})} 
                        />
                      </div>
                      <div className="form-group">
                        <label>Email Address</label>
                        <input 
                          type="email" 
                          value={patientRegForm.email} 
                          onChange={(e) => setPatientRegForm({...patientRegForm, email: e.target.value})} 
                          placeholder="patient@gmail.com"
                        />
                      </div>
                      <div className="form-group">
                        <label>Address</label>
                        <textarea
                          value={patientRegForm.address}
                          onChange={(e) => setPatientRegForm({...patientRegForm, address: e.target.value})}
                          rows={2}
                        />
                      </div>

                      {isNewbornMode && (
                        <>
                          <div className="form-group">
                            <label>Mother's ID Number</label>
                            <input
                              type="text"
                              value={patientRegForm.motherIdNumber}
                              onChange={(e) => setPatientRegForm({...patientRegForm, motherIdNumber: e.target.value})}
                              placeholder="Links this file to the mother's record"
                            />
                          </div>
                          <div className="form-group" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                            <div>
                              <label>Birth Weight (g)</label>
                              <input
                                type="number"
                                value={patientRegForm.birthWeightGrams}
                                onChange={(e) => setPatientRegForm({...patientRegForm, birthWeightGrams: e.target.value})}
                                placeholder="e.g. 3200"
                              />
                            </div>
                            <div>
                              <label>Birth Length (cm)</label>
                              <input
                                type="number"
                                step="0.1"
                                value={patientRegForm.birthLengthCm}
                                onChange={(e) => setPatientRegForm({...patientRegForm, birthLengthCm: e.target.value})}
                                placeholder="e.g. 49.5"
                              />
                            </div>
                          </div>
                          <div className="form-group" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                            <div>
                              <label>Apgar Score (1 min)</label>
                              <input
                                type="number"
                                min="0"
                                max="10"
                                value={patientRegForm.apgarScore1Min}
                                onChange={(e) => setPatientRegForm({...patientRegForm, apgarScore1Min: e.target.value})}
                              />
                            </div>
                            <div>
                              <label>Apgar Score (5 min)</label>
                              <input
                                type="number"
                                min="0"
                                max="10"
                                value={patientRegForm.apgarScore5Min}
                                onChange={(e) => setPatientRegForm({...patientRegForm, apgarScore5Min: e.target.value})}
                              />
                            </div>
                          </div>
                          <div style={{ background: 'rgba(14, 165, 233, 0.1)', padding: '10px', borderRadius: '8px', marginBottom: '16px' }}>
                            <p style={{ fontSize: '0.75rem', color: '#7dd3fc' }}>
                              💡 Birth facility is recorded as your current facility. The EPI immunization schedule (BCG, OPV, Rotavirus, PCV...) is generated automatically on save.
                            </p>
                          </div>
                        </>
                      )}

                      <button type="submit" className="btn btn-secondary" style={{ width: '100%' }}>
                        {isNewbornMode ? 'Open Newborn File' : 'Create Record'}
                      </button>
                    </form>
                  </div>
                </div>

                {/* Patient File display and clinical actions */}
                <div className="dashboard-main">
                  {searchedPatientRecord ? (
                    <>
                      {/* Demographics Card */}
                      <div className="glass-card" style={{ textAlign: 'left', display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: '20px' }}>
                        <div>
                          <h2 style={{ color: '#fff' }}>Patient File: {searchedPatientRecord.patient.firstName} {searchedPatientRecord.patient.lastName}</h2>
                          <p className="text-muted" style={{ marginTop: '4px' }}>
                            File Number (UHID): <strong style={{ color: '#fff' }}>{searchedPatientRecord.patient.uhid}</strong>
                            {!searchedPatientRecord.patient.idNumber && (
                              <span style={{ color: '#fde047', marginLeft: '8px' }}>⚠️ No ID number registered yet</span>
                            )}
                          </p>
                          <p className="text-muted" style={{ marginTop: '4px' }}>
                            ID Number: {searchedPatientRecord.patient.idNumber || 'Not yet issued'} | Gender: {searchedPatientRecord.patient.gender} | DOB: {searchedPatientRecord.patient.dateOfBirth}
                          </p>
                          {searchedPatientRecord.patient.motherPatient && (
                            <p className="text-muted" style={{ marginTop: '4px', color: '#c7d2fe' }}>
                              👶 Mother: {searchedPatientRecord.patient.motherPatient.firstName} {searchedPatientRecord.patient.motherPatient.lastName} (ID: {searchedPatientRecord.patient.motherPatient.idNumber || searchedPatientRecord.patient.motherPatient.uhid})
                            </p>
                          )}
                          {searchedPatientRecord.patient.birthWeightGrams && (
                            <p className="text-muted" style={{ marginTop: '4px' }}>
                              🍼 Born at {searchedPatientRecord.patient.birthFacility?.name || 'N/A'}: {searchedPatientRecord.patient.birthWeightGrams}g, {searchedPatientRecord.patient.birthLengthCm}cm, Apgar {searchedPatientRecord.patient.apgarScore1Min}/{searchedPatientRecord.patient.apgarScore5Min}
                            </p>
                          )}
                          <p className="text-muted" style={{ marginTop: '4px' }}>
                            Contact: {searchedPatientRecord.patient.contactNumber || 'N/A'} | Email: {searchedPatientRecord.patient.email || 'N/A'}
                          </p>
                          <p className="text-muted" style={{ marginTop: '4px' }}>
                            Address: {searchedPatientRecord.patient.address || 'N/A'}
                          </p>
                          {(() => {
                            const lastVisit = searchedPatientRecord.visits && searchedPatientRecord.visits.length > 0 
                              ? searchedPatientRecord.visits.reduce((latest, current) => 
                                  new Date(current.visitDate) > new Date(latest.visitDate) ? current : latest
                                )
                              : null;
                            return lastVisit ? (
                              <p className="text-muted" style={{ marginTop: '8px', color: '#c7d2fe', fontSize: '0.85rem' }}>
                                📅 <strong>Last Visit:</strong> {new Date(lastVisit.visitDate).toLocaleDateString()} — <em>{lastVisit.reason} {lastVisit.notes ? `(${lastVisit.notes})` : ''}</em>
                              </p>
                            ) : (
                              <p className="text-muted" style={{ marginTop: '8px', fontSize: '0.85rem' }}>
                                📅 <strong>Last Visit:</strong> No recorded visits
                              </p>
                            );
                          })()}
                        </div>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', alignItems: 'flex-end' }}>
                          <span className="badge badge-green">Status: Active</span>
                          <button 
                            className="btn btn-primary" 
                            onClick={() => handleEvaluatePatient(searchedPatientRecord.patient.id)}
                            style={{ padding: '6px 12px', fontSize: '0.8rem', display: 'flex', alignItems: 'center', gap: '6px', marginTop: '6px' }}
                          >
                            <RefreshCw size={14} /> Analyze Response (CDS)
                          </button>
                        </div>
                      </div>

                      {/* Treatment Response Timeline (CDS View) */}
                      {patientTimeline && (
                        <div className="glass-card" style={{ textAlign: 'left' }}>
                          <h3 style={{ color: '#fff', fontSize: '1.25rem', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <Clock className="text-secondary" style={{ color: '#0ea5e9' }} /> 📈 Treatment Response Timeline (Past 14 Days)
                          </h3>

                          {/* Display active alerts if any */}
                          {patientTimeline.activeAlerts && patientTimeline.activeAlerts.length > 0 && (
                            <div style={{ marginBottom: '16px', display: 'flex', flexDirection: 'column', gap: '8px' }}>
                              {patientTimeline.activeAlerts.map(alert => (
                                <div key={alert.id} style={{ background: 'rgba(239, 68, 68, 0.1)', border: '1px solid rgba(239, 68, 68, 0.2)', borderRadius: '8px', padding: '12px' }}>
                                  <p style={{ color: '#fca5a5', fontSize: '0.85rem', fontWeight: 600 }}>🚨 Clinical Alert: {alert.alertType}</p>
                                  <p style={{ color: '#fff', fontSize: '0.8rem', marginTop: '2px' }}>{alert.message}</p>
                                </div>
                              ))}
                            </div>
                          )}

                          {/* Display Food conflicts if any */}
                          {patientTimeline.foodConflicts && patientTimeline.foodConflicts.length > 0 && (
                            <div style={{ marginBottom: '16px', background: 'rgba(245, 158, 11, 0.08)', border: '1px solid rgba(245, 158, 11, 0.2)', borderRadius: '8px', padding: '12px' }}>
                              <p style={{ color: '#fde047', fontSize: '0.85rem', fontWeight: 600 }}>⚠️ Drug-Food Interactions Detected</p>
                              <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', marginTop: '6px' }}>
                                {patientTimeline.foodConflicts.map((c, i) => (
                                  <p key={i} style={{ fontSize: '0.75rem', color: '#fff' }}>
                                    - <strong>{c.medication}</strong> conflicts with scanned ingredient <strong>{c.ingredient}</strong> ({c.message})
                                  </p>
                                ))}
                              </div>
                            </div>
                          )}

                          {/* Timeline Table Grid */}
                          <div className="grid grid-cols-2" style={{ gap: '16px' }}>
                            <div>
                              <h4 style={{ color: '#a5b4fc', fontSize: '0.9rem', marginBottom: '8px' }}>Medication Doses (Adherence logs)</h4>
                              {patientTimeline.adherenceLogs.length === 0 ? (
                                <p className="text-muted" style={{ fontSize: '0.8rem' }}>No doses logged in last 14 days.</p>
                              ) : (
                                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', maxHeight: '200px', overflowY: 'auto', background: 'rgba(15,23,42,0.3)', padding: '8px', borderRadius: '8px' }}>
                                  {patientTimeline.adherenceLogs.map(log => (
                                    <div key={log.id} style={{ display: 'flex', flexDirection: 'column', padding: '6px', borderBottom: '1px solid rgba(255,255,255,0.03)', gap: '2px' }}>
                                      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', alignItems: 'center' }}>
                                        <span style={{ color: '#fff' }}>{log.reminder.prescription.medication}</span>
                                        <span style={{ color: 'var(--text-muted)' }}>{new Date(log.scheduledTime).toLocaleDateString([], { month: 'short', day: 'numeric' })} {log.reminder.reminderTime}</span>
                                        <span className={`badge ${log.status === 'TAKEN' ? 'badge-green' : log.status === 'MISSED' ? 'badge-red' : 'badge-yellow'}`} style={{ padding: '2px 6px', fontSize: '0.65rem' }}>
                                          {log.status}
                                        </span>
                                      </div>
                                      {log.notes && (
                                        <p style={{ fontSize: '0.7rem', color: '#fca5a5', fontStyle: 'italic', margin: '0' }}>
                                          Feedback: "{log.notes}"
                                        </p>
                                      )}
                                    </div>
                                  ))}
                                </div>
                              )}
                            </div>

                            <div>
                              <h4 style={{ color: '#a5b4fc', fontSize: '0.9rem', marginBottom: '8px' }}>Symptom Check History</h4>
                              {patientTimeline.symptomChecks.length === 0 ? (
                                <p className="text-muted" style={{ fontSize: '0.8rem' }}>No symptom checks logged in last 14 days.</p>
                              ) : (
                                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', maxHeight: '200px', overflowY: 'auto', background: 'rgba(15,23,42,0.3)', padding: '8px', borderRadius: '8px' }}>
                                  {patientTimeline.symptomChecks.map(check => (
                                    <div key={check.id} style={{ display: 'flex', flexDirection: 'column', padding: '6px', borderBottom: '1px solid rgba(255,255,255,0.03)', fontSize: '0.75rem', gap: '4px' }}>
                                      <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                                        <span style={{ color: 'var(--text-muted)' }}>{new Date(check.checkedAt).toLocaleDateString()}</span>
                                        <span style={{ color: check.urgencyLevel === 'RED' ? 'var(--danger)' : check.urgencyLevel === 'YELLOW' ? 'var(--warning)' : 'var(--success)' }}>
                                          {check.urgencyLevel}
                                        </span>
                                      </div>
                                      <p style={{ color: '#fff', margin: '0', fontSize: '0.75rem' }}>{check.recommendation.split('[')[0]}</p>
                                      {check.details && check.details.length > 0 && (
                                        <div style={{ display: 'flex', gap: '4px', flexWrap: 'wrap', marginTop: '2px' }}>
                                          {check.details.map((d, idx) => (
                                            <span key={idx} style={{ background: 'rgba(255, 255, 255, 0.05)', color: '#94a3b8', border: '1px solid rgba(255,255,255,0.08)', padding: '1px 6px', borderRadius: '3px', fontSize: '0.65rem' }}>
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
                        </div>
                      )}

                      {/* Medical Record sections */}
                      <div className="grid grid-cols-2">
                        
                        {/* Active Conditions and Allergies */}
                        <div className="glass-card" style={{ textAlign: 'left' }}>
                          <h3 style={{ color: '#fff', fontSize: '1.2rem', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <Clipboard /> Chronic Conditions & Allergies
                          </h3>

                          {/* Conditions list */}
                          <h4 style={{ color: '#a5b4fc', fontSize: '0.9rem', marginBottom: '8px' }}>Active Chronic Conditions</h4>
                          {searchedPatientRecord.chronicConditions.length === 0 ? (
                            <p className="text-muted" style={{ fontSize: '0.85rem', marginBottom: '16px' }}>No registered chronic conditions.</p>
                          ) : (
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginBottom: '16px' }}>
                              {searchedPatientRecord.chronicConditions.map(c => (
                                <div key={c.id} style={{ background: 'rgba(79, 70, 229, 0.05)', border: '1px solid rgba(79, 70, 229, 0.1)', padding: '10px', borderRadius: '8px' }}>
                                  <p style={{ color: '#fff', fontWeight: 600, fontSize: '0.85rem' }}>{c.conditionName}</p>
                                  <p className="text-muted" style={{ fontSize: '0.75rem' }}>Diagnosed: {c.diagnosedDate} | {c.notes}</p>
                                </div>
                              ))}
                            </div>
                          )}

                          {/* Allergies list */}
                          <h4 style={{ color: '#fca5a5', fontSize: '0.9rem', marginBottom: '8px' }}>Allergies</h4>
                          {searchedPatientRecord.allergies.length === 0 ? (
                            <p className="text-muted" style={{ fontSize: '0.85rem' }}>No registered drug or food allergies.</p>
                          ) : (
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                              {searchedPatientRecord.allergies.map(a => (
                                <div key={a.id} style={{ background: 'rgba(239, 68, 68, 0.05)', border: '1px solid rgba(239, 68, 68, 0.1)', padding: '10px', borderRadius: '8px' }}>
                                  <p style={{ color: '#fff', fontWeight: 600, fontSize: '0.85rem' }}>{a.allergen}</p>
                                  <p className="text-muted" style={{ fontSize: '0.75rem' }}>Severity: {a.severity} | {a.notes}</p>
                                </div>
                              ))}
                            </div>
                          )}
                        </div>

                        {/* Prescriptions and Adherence */}
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
                          {/* Active Prescriptions */}
                          <div className="glass-card" style={{ textAlign: 'left' }}>
                            <h3 style={{ color: '#fff', fontSize: '1.2rem', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                              <Pill /> Active Prescriptions
                            </h3>
                            {searchedPatientRecord.prescriptions.length === 0 ? (
                              <p className="text-muted" style={{ fontSize: '0.85rem' }}>No prescriptions active.</p>
                            ) : (
                              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                                {searchedPatientRecord.prescriptions.map(p => (
                                  <div key={p.id} style={{ background: 'rgba(15, 23, 42, 0.4)', border: '1px solid rgba(255,255,255,0.05)', padding: '10px', borderRadius: '8px' }}>
                                    <p style={{ color: '#fff', fontWeight: 600, fontSize: '0.85rem' }}>{p.medication}</p>
                                    <p className="text-muted" style={{ fontSize: '0.75rem' }}>Dosage: {p.dosage} | Frequency: {p.frequency} | Duration: {p.durationDays} days</p>
                                    <p className="text-muted" style={{ fontSize: '0.7rem', marginTop: '2px' }}>Notes: {p.notes}</p>
                                  </div>
                                ))}
                              </div>
                            )}
                          </div>

                          {/* Medication Adherence Logs (Doctor view) */}
                          {patientAdherence && patientAdherence.stats.totalDoses > 0 && (
                            <div className="glass-card" style={{ textAlign: 'left' }}>
                              <h3 style={{ color: '#fff', fontSize: '1.2rem', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                                <AlertCircle style={{ color: '#f59e0b' }} /> Patient Adherence History
                              </h3>
                              
                              <div style={{ background: 'rgba(15, 23, 42, 0.4)', padding: '12px', borderRadius: '12px', border: '1px solid rgba(255,255,255,0.05)', marginBottom: '16px' }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                  <span style={{ fontSize: '0.85rem', color: '#fff' }}>Adherence Compliance Score</span>
                                  <span style={{ fontSize: '1.1rem', fontWeight: 'bold', color: patientAdherence.stats.adherenceScore >= 80 ? 'var(--success)' : 'var(--warning)' }}>
                                    {patientAdherence.stats.adherenceScore}%
                                  </span>
                                </div>
                                <p className="text-muted" style={{ fontSize: '0.75rem', marginTop: '4px' }}>
                                  Taken {patientAdherence.stats.takenDoses} of {patientAdherence.stats.totalDoses} doses this week.
                                </p>
                              </div>

                              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', maxHeight: '200px', overflowY: 'auto' }}>
                                {patientAdherence.adherenceLogs.map((log) => {
                                  const timeString = new Date(log.scheduledTime).toLocaleString([], { dateStyle: 'short', timeStyle: 'short' });
                                  return (
                                    <div key={log.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px', borderRadius: '8px', background: 'rgba(15, 23, 42, 0.3)', border: '1px solid rgba(255,255,255,0.03)' }}>
                                      <div>
                                        <p style={{ color: '#fff', fontWeight: 600, fontSize: '0.8rem' }}>{log.reminder.prescription.medication}</p>
                                        <p className="text-muted" style={{ fontSize: '0.7rem' }}>Scheduled: {timeString}</p>
                                        {log.notes && (
                                          <p style={{ fontSize: '0.75rem', color: '#fca5a5', marginTop: '2px', fontStyle: 'italic' }}>
                                            Patient Feedback: "{log.notes}"
                                          </p>
                                        )}
                                      </div>
                                      <span className={`badge ${log.status === 'TAKEN' ? 'badge-green' : log.status === 'MISSED' ? 'badge-red' : 'badge-yellow'}`}>
                                        {log.status}
                                      </span>
                                    </div>
                                  );
                                })}
                              </div>
                            </div>
                          )}
                        </div>
                      </div>

                      {/* Diagnoses and Lab Results */}
                      <div className="glass-card" style={{ textAlign: 'left' }}>
                        <h3 style={{ color: '#fff', fontSize: '1.2rem', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                          <FileText /> Diagnostic Logs & Clinical Visits
                        </h3>
                        {searchedPatientRecord.diagnoses.length === 0 ? (
                          <p className="text-muted" style={{ fontSize: '0.85rem' }}>No diagnoses recorded.</p>
                        ) : (
                          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                            {searchedPatientRecord.diagnoses.map(d => (
                              <div key={d.id} style={{ background: 'rgba(15, 23, 42, 0.4)', padding: '12px', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.03)' }}>
                                <p style={{ color: '#fff', fontSize: '0.9rem', fontWeight: 600 }}>
                                  {d.diagnosis} {d.icd10Code && <span style={{ color: '#38bdf8', fontSize: '0.75rem', fontWeight: 'normal', background: 'rgba(56, 189, 248, 0.1)', padding: '2px 6px', borderRadius: '4px', marginLeft: '6px' }}>ICD-10: {d.icd10Code}</span>}
                                </p>
                                <p className="text-muted" style={{ fontSize: '0.8rem' }}>Diagnosed: {new Date(d.diagnosedAt).toLocaleString()} | Notes: {d.notes}</p>
                              </div>
                            ))}
                          </div>
                        )}
                      </div>

                      {/* Immunization Schedule (EPI) */}
                      <div className="glass-card" style={{ textAlign: 'left' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px', flexWrap: 'wrap', gap: '10px' }}>
                          <h3 style={{ color: '#fff', fontSize: '1.2rem', display: 'flex', alignItems: 'center', gap: '8px', margin: 0 }}>
                            <Shield /> Immunization Schedule (EPI)
                          </h3>
                          <div style={{ display: 'flex', gap: '8px' }}>
                            {(!searchedPatientRecord.immunizations || searchedPatientRecord.immunizations.length === 0) && (
                              <button
                                className="btn btn-secondary"
                                onClick={() => handleGenerateImmunizationSchedule(searchedPatientRecord.patient.id)}
                                style={{ padding: '6px 12px', fontSize: '0.8rem' }}
                                disabled={loading}
                              >
                                Generate EPI Schedule
                              </button>
                            )}
                            <button
                              className="btn btn-secondary"
                              onClick={() => setShowCatchUpForm(!showCatchUpForm)}
                              style={{ padding: '6px 12px', fontSize: '0.8rem' }}
                            >
                              {showCatchUpForm ? 'Cancel' : '+ Add Catch-up Record'}
                            </button>
                          </div>
                        </div>

                        {showCatchUpForm && (
                          <form
                            onSubmit={(e) => handleAddCatchUpImmunization(e, searchedPatientRecord.patient.id)}
                            style={{ background: 'rgba(15, 23, 42, 0.4)', border: '1px solid rgba(255,255,255,0.05)', borderRadius: '10px', padding: '16px', marginBottom: '16px' }}
                          >
                            <p className="text-muted" style={{ fontSize: '0.8rem', marginBottom: '12px' }}>
                              Log a vaccine given outside the standard EPI schedule — a dose administered at another facility before this file existed, a travel vaccine, or a catch-up dose.
                            </p>
                            <div className="form-group" style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '10px' }}>
                              <div>
                                <label>Vaccine Name</label>
                                <input
                                  type="text"
                                  value={catchUpForm.vaccineName}
                                  onChange={(e) => setCatchUpForm({...catchUpForm, vaccineName: e.target.value})}
                                  placeholder="e.g. Yellow Fever, Hepatitis B booster"
                                  required
                                />
                              </div>
                              <div>
                                <label>Dose Number</label>
                                <input
                                  type="number"
                                  min="0"
                                  value={catchUpForm.doseNumber}
                                  onChange={(e) => setCatchUpForm({...catchUpForm, doseNumber: e.target.value})}
                                  placeholder="optional"
                                />
                              </div>
                            </div>
                            <div className="form-group" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                              <div>
                                <label>Scheduled Date</label>
                                <input
                                  type="date"
                                  value={catchUpForm.scheduledDate}
                                  onChange={(e) => setCatchUpForm({...catchUpForm, scheduledDate: e.target.value})}
                                />
                              </div>
                              <div>
                                <label>Administered Date (leave blank if still due)</label>
                                <input
                                  type="date"
                                  value={catchUpForm.administeredDate}
                                  onChange={(e) => setCatchUpForm({...catchUpForm, administeredDate: e.target.value})}
                                />
                              </div>
                            </div>
                            <div className="form-group">
                              <label>Notes</label>
                              <textarea
                                value={catchUpForm.notes}
                                onChange={(e) => setCatchUpForm({...catchUpForm, notes: e.target.value})}
                                rows={2}
                                placeholder="e.g. Given at Themba Hospital prior to this file being opened"
                              />
                            </div>
                            <button type="submit" className="btn btn-primary" disabled={loading}>
                              {loading ? 'Saving...' : 'Save Record'}
                            </button>
                          </form>
                        )}

                        {(!searchedPatientRecord.immunizations || searchedPatientRecord.immunizations.length === 0) ? (
                          <p className="text-muted" style={{ fontSize: '0.85rem' }}>No immunization schedule on file. Generate one to start tracking EPI doses for this patient.</p>
                        ) : (
                          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                            {searchedPatientRecord.immunizations.map(dose => (
                              <div
                                key={dose.id}
                                style={{
                                  display: 'flex',
                                  justifyContent: 'space-between',
                                  alignItems: 'center',
                                  padding: '10px 12px',
                                  borderRadius: '8px',
                                  background: dose.status === 'GIVEN' ? 'rgba(16, 185, 129, 0.06)' : dose.status === 'MISSED' ? 'rgba(239, 68, 68, 0.06)' : 'rgba(15, 23, 42, 0.4)',
                                  border: '1px solid rgba(255,255,255,0.05)'
                                }}
                              >
                                <div>
                                  <p style={{ color: '#fff', fontWeight: 600, fontSize: '0.9rem' }}>
                                    {dose.vaccineName} {dose.doseNumber != null && `(Dose ${dose.doseNumber})`}
                                  </p>
                                  <p className="text-muted" style={{ fontSize: '0.75rem' }}>
                                    Scheduled: {dose.scheduledDate}{dose.administeredDate && ` | Given: ${dose.administeredDate}`}
                                  </p>
                                </div>
                                <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                                  {dose.status === 'DUE' ? (
                                    <>
                                      <button className="btn btn-success" onClick={() => handleAdministerDose(dose.id)} style={{ padding: '6px 10px', fontSize: '0.75rem', display: 'flex', alignItems: 'center', gap: '4px' }} disabled={loading}>
                                        <CheckCircle size={14} /> Given
                                      </button>
                                      <button className="btn btn-danger" onClick={() => handleMissDose(dose.id)} style={{ padding: '6px 10px', fontSize: '0.75rem', display: 'flex', alignItems: 'center', gap: '4px' }} disabled={loading}>
                                        <XCircle size={14} /> Missed
                                      </button>
                                    </>
                                  ) : (
                                    <span className={`badge ${dose.status === 'GIVEN' ? 'badge-green' : 'badge-red'}`}>{dose.status}</span>
                                  )}
                                </div>
                              </div>
                            ))}
                          </div>
                        )}
                      </div>

                      {/* Clinical Actions Form Panels */}
                      <div className="grid grid-cols-3" style={{ gap: '20px' }}>
                        
                        {/* Add Diagnosis Form */}
                        <div className="glass-card" style={{ textAlign: 'left' }}>
                          <h3 style={{ color: '#fff', fontSize: '1.1rem', marginBottom: '16px' }}>Add Clinical Diagnosis</h3>
                          <form onSubmit={handleAddDiagnosis}>
                            <div className="form-group">
                              <label>Condition / Disease Name</label>
                              <input 
                                type="text" 
                                value={addDiagnosisForm.conditionName} 
                                onChange={(e) => setAddDiagnosisForm({...addDiagnosisForm, conditionName: e.target.value})} 
                                placeholder="e.g. Influenza, Gastritis"
                                required 
                              />
                            </div>
                            <div className="form-group">
                              <label>Clinical Notes</label>
                              <textarea 
                                value={addDiagnosisForm.notes} 
                                onChange={(e) => setAddDiagnosisForm({...addDiagnosisForm, notes: e.target.value})} 
                                placeholder="Patient reports acute onset..."
                                rows={3}
                              />
                            </div>
                            <button type="submit" className="btn btn-secondary" style={{ width: '100%' }}>
                              Save Diagnosis
                            </button>
                          </form>
                        </div>

                        {/* Add Prescription Form */}
                        <div className="glass-card" style={{ textAlign: 'left' }}>
                          <h3 style={{ color: '#fff', fontSize: '1.1rem', marginBottom: '16px' }}>Issue Prescription</h3>
                          <form onSubmit={handleAddPrescription}>
                            <div className="form-group">
                              <label>Medication Name</label>
                              <input 
                                type="text" 
                                value={addPrescriptionForm.medicationName} 
                                onChange={(e) => setAddPrescriptionForm({...addPrescriptionForm, medicationName: e.target.value})} 
                                placeholder="e.g. Paracetamol 500mg, Amoxicillin 250mg"
                                required 
                              />
                            </div>
                            <div className="form-group" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                              <div>
                                  <label>Dosage</label>
                                  <input 
                                    type="text" 
                                    value={addPrescriptionForm.dosage} 
                                    onChange={(e) => setAddPrescriptionForm({...addPrescriptionForm, dosage: e.target.value})} 
                                    placeholder="e.g. 1 Tablet"
                                    required 
                                  />
                              </div>
                              <div>
                                  <label>Frequency</label>
                                  <select 
                                    value={addPrescriptionForm.frequency} 
                                    onChange={(e) => setAddPrescriptionForm({...addPrescriptionForm, frequency: e.target.value})}
                                    required
                                  >
                                    <option value="Once daily">Once daily</option>
                                    <option value="Twice daily">Twice daily</option>
                                    <option value="Three times daily">Three times daily</option>
                                    <option value="With meals">With meals</option>
                                  </select>
                              </div>
                            </div>
                            <div className="form-group">
                              <label>Duration (Days)</label>
                              <input 
                                type="number" 
                                value={addPrescriptionForm.durationDays} 
                                onChange={(e) => setAddPrescriptionForm({...addPrescriptionForm, durationDays: parseInt(e.target.value) || 7})} 
                                required 
                              />
                            </div>
                            <button type="submit" className="btn btn-secondary" style={{ width: '100%' }}>
                              Issue Prescription
                            </button>
                          </form>
                        </div>

                        {/* Add Clinical Alert Form */}
                        <div className="glass-card" style={{ textAlign: 'left' }}>
                          <h3 style={{ color: '#fff', fontSize: '1.1rem', marginBottom: '16px' }}>Add Clinical Alert / Warning</h3>
                          <form onSubmit={handleAddAlert}>
                            <div className="form-group">
                              <label>Severity Level</label>
                              <select 
                                value={addAlertForm.severity} 
                                onChange={(e) => setAddAlertForm({...addAlertForm, severity: e.target.value})}
                                required
                              >
                                <option value="LOW">Low</option>
                                <option value="MEDIUM">Medium</option>
                                <option value="HIGH">High</option>
                                <option value="CRITICAL">Critical</option>
                              </select>
                            </div>
                            <div className="form-group">
                              <label>Alert Message / Instruction</label>
                              <textarea 
                                value={addAlertForm.message} 
                                onChange={(e) => setAddAlertForm({...addAlertForm, message: e.target.value})} 
                                placeholder="Patient reports severe dizziness when taking Metformin..."
                                rows={4}
                                required
                              />
                            </div>
                            <button type="submit" className="btn btn-secondary" style={{ width: '100%' }}>
                              Save Clinical Alert
                            </button>
                          </form>
                        </div>
                      </div>
                    </>
                  ) : (
                    <div className="glass-card" style={{ padding: '80px 20px', textAlign: 'center' }}>
                      <Clipboard size={64} className="text-muted" style={{ margin: '0 auto 16px', opacity: 0.3 }} />
                      <h3 style={{ color: '#fff', marginBottom: '8px' }}>No Patient File Loaded</h3>
                      <p className="text-muted">Use the lookup tool on the left to locate a patient by their national ID number or register a new patient.</p>
                    </div>
                  )}
                </div>
              </div>
            ) : activeTabStaff === 'alerts' ? (
              /* Feature 5: Clinical Alerts Feed Layout */
              <div style={{ maxWidth: '1200px', margin: '0 auto 40px', padding: '0 20px', textAlign: 'left' }}>
                <div className="glass-card">
                  <h2 style={{ fontSize: '1.5rem', color: '#fff', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <ShieldAlert style={{ color: '#ef4444' }} /> Active Clinical Alerts & Decision Support Feed
                  </h2>
                  <p className="text-muted" style={{ marginBottom: '24px', fontSize: '0.9rem' }}>
                    These alerts are automatically fired by the UDHR engine when a patient shows high medication adherence ($ge 90\%$) with poor clinical response (symptoms persisting at Red/Yellow urgency), or high-risk drug-food interactions.
                  </p>

                  {clinicalAlerts.length === 0 ? (
                    <div style={{ textAlign: 'center', padding: '60px 20px' }}>
                      <CheckCircle size={48} style={{ color: 'var(--success)', margin: '0 auto 12px', opacity: 0.6 }} />
                      <h4 style={{ color: '#fff' }}>No Active Clinical Alerts</h4>
                      <p className="text-muted" style={{ fontSize: '0.85rem', marginTop: '4px' }}>All monitored patients are responding well to treatment and have no dietary conflicts.</p>
                    </div>
                  ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
                      {clinicalAlerts.map((item) => {
                        const alert = item.alert;
                        return (
                          <div 
                            key={alert.id} 
                            style={{ 
                              border: `1px solid ${alert.severity === 'CRITICAL' ? 'rgba(239, 68, 68, 0.3)' : alert.severity === 'HIGH' ? 'rgba(245, 158, 11, 0.3)' : 'rgba(255,255,255,0.08)'}`,
                              background: alert.severity === 'CRITICAL' ? 'rgba(239, 68, 68, 0.05)' : alert.severity === 'HIGH' ? 'rgba(245, 158, 11, 0.04)' : 'rgba(15, 23, 42, 0.4)',
                              borderRadius: '16px',
                              padding: '24px'
                            }}
                          >
                            {/* Alert Header */}
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '12px', borderBottom: '1px solid rgba(255,255,255,0.05)', paddingBottom: '16px', marginBottom: '16px' }}>
                              <div>
                                <span className={`badge ${alert.severity === 'CRITICAL' ? 'badge-red' : 'badge-yellow'}`} style={{ fontSize: '0.7rem', padding: '2px 8px', fontWeight: 'bold' }}>
                                  {alert.severity} SEVERITY
                                </span>
                                <h3 style={{ color: '#fff', fontSize: '1.2rem', marginTop: '6px' }}>
                                  Patient: {item.patient.firstName} {item.patient.lastName} ({item.patient.idNumber})
                                </h3>
                                <p className="text-muted" style={{ fontSize: '0.8rem', marginTop: '4px' }}>
                                  Fired: {new Date(alert.createdAt).toLocaleString()} | Alert Type: {alert.alertType}
                                </p>
                              </div>
                              <button className="btn btn-success" onClick={() => handleResolveAlert(alert.id)}>
                                Resolve Alert & Clear
                              </button>
                            </div>

                            {/* Alert Details Body */}
                            <p style={{ color: '#fff', fontSize: '0.95rem', lineHeight: 1.6, marginBottom: '16px', fontStyle: 'italic', background: 'rgba(0,0,0,0.2)', padding: '12px', borderRadius: '8px' }}>
                              {alert.message}
                            </p>

                            {/* Compliance and Symptoms correlation details */}
                            <div className="grid grid-cols-2" style={{ gap: '20px', marginBottom: '20px' }}>
                              <div>
                                <h4 style={{ color: '#a5b4fc', fontSize: '0.85rem', marginBottom: '8px' }}>Patient Adherence & Prescriptions</h4>
                                <p style={{ color: '#fff', fontSize: '0.85rem' }}>
                                  Compliance score (last 14 days): <strong>{item.adherenceScore}%</strong>
                                </p>
                                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px', marginTop: '8px' }}>
                                  {item.activePrescriptions.map(p => (
                                    <span key={p.id} style={{ background: 'rgba(255,255,255,0.05)', padding: '4px 8px', borderRadius: '6px', fontSize: '0.75rem', color: '#fff', border: '1px solid rgba(255,255,255,0.05)' }}>
                                      💊 {p.medication} ({p.dosage})
                                    </span>
                                  ))}
                                </div>
                              </div>

                              <div>
                                <h4 style={{ color: '#a5b4fc', fontSize: '0.85rem', marginBottom: '8px' }}>Recent Symptom Checks</h4>
                                {item.recentSymptomChecks.length === 0 ? (
                                  <p className="text-muted" style={{ fontSize: '0.8rem' }}>No checks logged.</p>
                                ) : (
                                  <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                                    {item.recentSymptomChecks.map(check => (
                                      <div key={check.id} style={{ fontSize: '0.75rem', color: '#fff', background: 'rgba(0,0,0,0.1)', padding: '6px', borderRadius: '6px' }}>
                                        <strong>{new Date(check.checkedAt).toLocaleDateString()}:</strong> Urgency <span style={{ color: check.urgencyLevel === 'RED' ? 'var(--danger)' : 'var(--warning)' }}>{check.urgencyLevel}</span>
                                      </div>
                                    ))}
                                  </div>
                                )}
                              </div>
                            </div>

                            {/* CDSS Diagnostic Recommendations */}
                            {alert.alertType === 'NON_RESPONSE' && (
                              <div style={{ background: 'rgba(15,23,42,0.4)', borderRadius: '12px', padding: '16px', border: '1px solid rgba(255,255,255,0.03)' }}>
                                <h4 style={{ color: '#38bdf8', fontSize: '0.95rem', marginBottom: '12px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                                  <Heart size={16} /> Clinical Decision Support Recommendations
                                </h4>

                                {/* Lab Test suggestion */}
                                {item.labRecommendations && item.labRecommendations.length > 0 && (
                                  <div style={{ marginBottom: '12px', borderBottom: '1px solid rgba(255,255,255,0.03)', paddingBottom: '12px' }}>
                                    <h5 style={{ color: '#fff', fontSize: '0.85rem', fontWeight: 600 }}>Suggested Laboratory Diagnostics:</h5>
                                    {item.labRecommendations.map(lr => (
                                      <div key={lr.id} style={{ marginTop: '6px' }}>
                                        <p style={{ color: '#7dd3fc', fontSize: '0.85rem' }}>👉 Order: <strong>{lr.testName}</strong> {lr.icdCode && `(ICD-10: ${lr.icdCode})`}</p>
                                        <p className="text-muted" style={{ fontSize: '0.75rem', marginTop: '2px' }}><strong>Reasoning:</strong> {lr.reason}</p>
                                      </div>
                                    ))}
                                  </div>
                                )}

                                {/* Differential Diagnoses suggestions */}
                                {item.differentialDiagnoses && item.differentialDiagnoses.length > 0 && (
                                  <div>
                                    <h5 style={{ color: '#fff', fontSize: '0.85rem', fontWeight: 600 }}>Suggested ICD-10 Differential Diagnoses:</h5>
                                    <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginTop: '6px' }}>
                                      {item.differentialDiagnoses.map(dd => (
                                        <div key={dd.id} style={{ background: 'rgba(0,0,0,0.15)', padding: '10px', borderRadius: '8px' }}>
                                          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                            <span style={{ color: '#fff', fontSize: '0.8rem', fontWeight: 600 }}>{dd.conditionName} (ICD-10: {dd.icdCode})</span>
                                            <span style={{ 
                                              fontSize: '0.65rem', 
                                              fontWeight: 'bold', 
                                              color: dd.likelihood === 'HIGH' ? 'var(--danger)' : dd.likelihood === 'MODERATE' ? 'var(--warning)' : 'var(--success)',
                                              background: 'rgba(255,255,255,0.03)',
                                              padding: '2px 6px',
                                              borderRadius: '4px'
                                            }}>
                                              LIKELIHOOD: {dd.likelihood}
                                            </span>
                                          </div>
                                          <p className="text-muted" style={{ fontSize: '0.75rem', marginTop: '4px' }}><strong>Evidence/Reasoning:</strong> {dd.reasoning}</p>
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
                </div>
              </div>
            ) : (
              /* Admin: Staff Management */
              <div className="dashboard-grid">
                <div className="dashboard-sidebar">
                  <div className="glass-card" style={{ textAlign: 'left' }}>
                    <h3 style={{ color: '#fff', fontSize: '1.2rem', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <PlusCircle size={18} /> Register Staff Member
                    </h3>
                    <form onSubmit={handleRegisterStaff}>
                      <div className="form-group">
                        <label>Staff Number</label>
                        <input
                          type="text"
                          value={staffRegForm.staffNumber}
                          onChange={(e) => setStaffRegForm({...staffRegForm, staffNumber: e.target.value})}
                          placeholder="e.g. NUR002"
                          required
                        />
                      </div>
                      <div className="form-group">
                        <label>First Name</label>
                        <input
                          type="text"
                          value={staffRegForm.firstName}
                          onChange={(e) => setStaffRegForm({...staffRegForm, firstName: e.target.value})}
                          required
                        />
                      </div>
                      <div className="form-group">
                        <label>Last Name</label>
                        <input
                          type="text"
                          value={staffRegForm.lastName}
                          onChange={(e) => setStaffRegForm({...staffRegForm, lastName: e.target.value})}
                          required
                        />
                      </div>
                      <div className="form-group">
                        <label>Role</label>
                        <select
                          value={staffRegForm.role}
                          onChange={(e) => setStaffRegForm({...staffRegForm, role: e.target.value})}
                        >
                          <option value="ADMIN">Admin</option>
                          <option value="DOCTOR">Doctor</option>
                          <option value="NURSE">Nurse</option>
                        </select>
                      </div>
                      <div className="form-group">
                        <label>Facility</label>
                        <select
                          value={staffRegForm.facilityId}
                          onChange={(e) => setStaffRegForm({...staffRegForm, facilityId: e.target.value})}
                          required
                        >
                          <option value="" disabled>Select facility</option>
                          {facilitiesList.map(f => (
                            <option key={f.id} value={f.id}>{f.name} ({f.province})</option>
                          ))}
                        </select>
                      </div>
                      <div className="form-group">
                        <label>Email</label>
                        <input
                          type="email"
                          value={staffRegForm.email}
                          onChange={(e) => setStaffRegForm({...staffRegForm, email: e.target.value})}
                          placeholder="staff@udhr.gov.za"
                          required
                        />
                      </div>
                      <div className="form-group">
                        <label>Temporary Password</label>
                        <input
                          type="password"
                          value={staffRegForm.password}
                          onChange={(e) => setStaffRegForm({...staffRegForm, password: e.target.value})}
                          required
                        />
                      </div>
                      <button type="submit" className="btn btn-secondary" style={{ width: '100%' }} disabled={loading}>
                        {loading ? 'Registering...' : 'Register Staff Member'}
                      </button>
                    </form>
                  </div>
                </div>

                <div className="dashboard-main">
                  <div className="glass-card" style={{ textAlign: 'left' }}>
                    <h3 style={{ color: '#fff', fontSize: '1.2rem', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <User size={18} /> All Staff ({staffList.length})
                    </h3>
                    {staffList.length === 0 ? (
                      <p className="text-muted" style={{ fontSize: '0.9rem' }}>No staff members found.</p>
                    ) : (
                      <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                        {staffList.map(s => (
                          <div
                            key={s.id}
                            style={{
                              display: 'flex',
                              justifyContent: 'space-between',
                              alignItems: 'center',
                              flexWrap: 'wrap',
                              gap: '10px',
                              padding: '12px 16px',
                              borderRadius: '10px',
                              background: s.active ? 'rgba(15, 23, 42, 0.4)' : 'rgba(239, 68, 68, 0.05)',
                              border: '1px solid rgba(255,255,255,0.05)'
                            }}
                          >
                            <div>
                              <p style={{ color: '#fff', fontWeight: 600, fontSize: '0.9rem' }}>
                                {s.firstName} {s.lastName} <span className="text-muted" style={{ fontWeight: 'normal' }}>({s.staffNumber})</span>
                              </p>
                              <p className="text-muted" style={{ fontSize: '0.8rem' }}>
                                {s.role} | {s.facility?.name || 'No facility'} | {s.email}
                              </p>
                            </div>
                            <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                              {s.active ? (
                                <>
                                  <span className="badge badge-green">Active</span>
                                  <button
                                    className="btn btn-danger"
                                    onClick={() => handleDeactivateStaff(s.id)}
                                    style={{ padding: '6px 10px', fontSize: '0.75rem' }}
                                    disabled={loading}
                                  >
                                    Deactivate
                                  </button>
                                </>
                              ) : (
                                <span className="badge badge-red">Inactive</span>
                              )}
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              </div>
            )}
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
