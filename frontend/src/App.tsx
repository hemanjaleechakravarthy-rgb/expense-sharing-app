import { useState } from "react";
import "./App.css";

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL?.trim() || "http://localhost:8082";

type Balance = {
  fromUser: string;
  toUser: string;
  amount: number;
  paid: boolean;
};

type Group = {
  id: number;
  name: string | null;
  createdBy: {
    id: number;
    name: string;
    email: string;
  };
};

type GroupMember = {
  id: number;
  user: {
    id: number;
    name: string;
    email: string;
  };
  group: {
    id: number;
    name: string;
    createdBy: {
      id: number;
      name: string;
      email: string;
    };
  };
};

type Expense = {
  id: number;
  description: string;
  amount: number;
  createdAt: string;
  paidBy: {
    id: number;
    name: string;
    email: string;
  };
};

type ExpenseSplit = {
  id: number;
  amount: number;
  paid: boolean;
  expense: {
    id: number;
    description: string;
    amount: number;
  };
  user: {
    id: number;
    name: string;
    email: string;
  };
};

type Reminder = {
  splitId: number;
  fromUser: string;
  toUser: string;
  fromEmail: string;
  toEmail: string;
  amount: number;
  message: string;
};

type GroupSummary = {
  groupId: number;
  groupName: string;
  totalExpenses: number;
  totalExpenseCount: number;
  myContributions: number;
  myShare: number;
  myPendingPayments: number;
};

type RazorpaySuccessResponse = {
  razorpay_order_id: string;
  razorpay_payment_id: string;
  razorpay_signature: string;
};

type RazorpayCheckoutOptions = {
  key: string;
  amount: number;
  currency: string;
  name: string;
  description: string;
  order_id: string;
  handler: (response: RazorpaySuccessResponse) => Promise<void>;
  theme: { color: string };
};

type WindowWithRazorpay = Window & {
  Razorpay?: new (options: RazorpayCheckoutOptions) => {
    open: () => void;
    on: (event: string, handler: (response: unknown) => void) => void;
  };
};


type PaymentHistoryItem = {
  id: number;
  description: string;
  group: string;
  paidTo: string;
  amount: number;
  paidAt: string | null;
  paymentId: string | null;
};


function App() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [paymentHistory, setPaymentHistory] = useState<PaymentHistoryItem[]>([]);
  const [paymentHistoryLoading, setPaymentHistoryLoading] = useState(false);
  const [paymentHistoryError, setPaymentHistoryError] = useState("");

  // =========================
  // SIGNUP STATES
  // =========================

  const [showSignup, setShowSignup] = useState(false);
  const [signupName, setSignupName] = useState("");
  const [signupEmail, setSignupEmail] = useState("");
  const [signupPassword, setSignupPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const [loggedIn, setLoggedIn] = useState(false);

  // =========================
  // DASHBOARD STATES
  // =========================

  const [balances, setBalances] = useState<Balance[]>([]);
  const [members, setMembers] = useState<GroupMember[]>([]);
  const [reminders, setReminders] = useState<Reminder[]>([]);
  const [summary, setSummary] = useState<GroupSummary | null>(null);

  const [groups, setGroups] = useState<Group[]>([]);
  const [activeGroupId, setActiveGroupId] = useState<number | null>(null);

  // Keeps track of the group that was active on the dashboard
  // before opening another group's page.
  const [dashboardGroupId, setDashboardGroupId] =
    useState<number | null>(null);

  const [showCreateGroup, setShowCreateGroup] = useState(false);
  const [newGroupName, setNewGroupName] = useState("");

  // =========================
  // GROUP PAGE STATES
  // =========================

  const [showGroupPage, setShowGroupPage] = useState(false);
  const [selectedGroup, setSelectedGroup] =
    useState<number | null>(null);

  const [showAddMember, setShowAddMember] = useState(false);
  const [newMemberName, setNewMemberName] = useState("");
  const [newMemberEmail, setNewMemberEmail] = useState("");

  // =========================
  // EXPENSE STATES
  // =========================

  const [expenses, setExpenses] = useState<Expense[]>([]);
  // Personal expenses are loaded from the authenticated user's own endpoint.
  const [personalExpenses, setPersonalExpenses] = useState<Expense[]>([]);
  const [showPersonalExpenseForm, setShowPersonalExpenseForm] = useState(false);
  const [personalExpenseDescription, setPersonalExpenseDescription] = useState("");
  const [personalExpenseAmount, setPersonalExpenseAmount] = useState("");
  const [showExpenses, setShowExpenses] = useState(false);

  const [showAddExpense, setShowAddExpense] = useState(false);
  const [expenseDescription, setExpenseDescription] = useState("");
  const [expenseAmount, setExpenseAmount] = useState("");

  const [paidByUserId, setPaidByUserId] = useState("3");

  const [selectedExpense, setSelectedExpense] =
    useState<Expense | null>(null);

  const [splits, setSplits] = useState<ExpenseSplit[]>([]);

  const [showSplitForm, setShowSplitForm] = useState(false);
  const [showEqualSplitWizard, setShowEqualSplitWizard] = useState(false);
  const [equalSplitGroupId, setEqualSplitGroupId] = useState<number | "">("");
  const [equalSplitMembers, setEqualSplitMembers] = useState<GroupMember[]>([]);
  const [equalSplitMembersLoading, setEqualSplitMembersLoading] = useState(false);
  const [equalSplitMembersError, setEqualSplitMembersError] = useState("");
  const [equalSplitDescription, setEqualSplitDescription] = useState("");
  const [equalSplitAmount, setEqualSplitAmount] = useState("");
  const [equalSplitExpenseId, setEqualSplitExpenseId] = useState<number | "">("");
  const [equalSplitPayerId, setEqualSplitPayerId] = useState("");
  const [equalSplitReview, setEqualSplitReview] = useState(false);
  // Per-member allocations are stored as integer paise to avoid floating-point rounding.
  const [equalSplitSharesPaise, setEqualSplitSharesPaise] = useState<Record<number, number>>({});
  const [equalSplitSubmitting, setEqualSplitSubmitting] = useState(false);
  const [equalSplitSubmissionKey, setEqualSplitSubmissionKey] = useState(() =>
  crypto.randomUUID()
);
  const [showTransactionHistory, setShowTransactionHistory] = useState(false);

  // =========================
  // LOAD GROUPS
  // =========================

  const loadGroups = async (): Promise<Group[]> => {
    const token = localStorage.getItem("token");

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/groups`,
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      if (!response.ok) {
        setGroups([]);
        return [];
      }

      const data: Group[] = await response.json();

      setGroups(data);

      return data;
    } catch (error) {
      console.error("Groups error:", error);
      setGroups([]);
      return [];
    }
  };

  // =========================
  // LOAD BALANCES
  // =========================

  const loadBalances = async (groupId?: number) => {
    const token = localStorage.getItem("token");
    const targetGroupId = groupId ?? activeGroupId;

    if (!targetGroupId) {
      setBalances([]);
      return;
    }

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/groups/${targetGroupId}/balances`,
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      if (!response.ok) {
        setBalances([]);
        return;
      }

      const data = await response.json();
      setBalances(data);
    } catch (error) {
      console.error("Balance error:", error);
      setBalances([]);
    }
  };

  // =========================
  // LOAD REMINDERS
  // =========================

  const loadReminders = async (_groupId?: number) => {
    const token = localStorage.getItem("token");

    if (!token) {
      setReminders([]);
      return;
    }

    try {
     const response = await fetch(
        `${API_BASE_URL}/api/groups/${_groupId}/reminders`,
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      if (!response.ok) {
        console.error(
          "Failed to load reminders:",
          response.status
        );
        setReminders([]);
        return;
      }

      const data: Reminder[] = await response.json();

      setReminders(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error("Reminders error:", error);
      setReminders([]);
    }
  };

  // =========================
  // LOAD SUMMARY
  // =========================

  const loadSummary = async (groupId?: number) => {
    const token = localStorage.getItem("token");
    const targetGroupId = groupId ?? activeGroupId;

    if (!targetGroupId) {
      setSummary(null);
      return;
    }

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/groups/${targetGroupId}/summary`,
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      if (!response.ok) {
        setSummary(null);
        return;
      }

      const data = await response.json();
      setSummary(data);
    } catch (error) {
      console.error("Summary error:", error);
      setSummary(null);
    }
  };

  // =========================
  // LOAD MEMBERS
  // =========================

  const loadMembers = async (groupId?: number) => {
    const token = localStorage.getItem("token");
    const targetGroupId = groupId ?? activeGroupId;

    if (!targetGroupId) {
      setMembers([]);
      return;
    }

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/groups/${targetGroupId}/members`,
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      if (!response.ok) {
        setMembers([]);
        return;
      }

      const data = await response.json();

      setMembers(data);

      if (data.length > 0) {
        const currentPaidByExists = data.some(
          (member: GroupMember) =>
            String(member.user.id) === paidByUserId
        );

        if (!currentPaidByExists) {
          setPaidByUserId(String(data[0].user.id));
        }
      }
    } catch (error) {
      console.error("Members error:", error);
      setMembers([]);
    }
  };

  // =========================
  // LOAD EXPENSES
  // =========================

  const loadExpenses = async (groupId?: number) => {
    const token = localStorage.getItem("token");
    const targetGroupId = groupId ?? activeGroupId;

    if (!targetGroupId) {
      setExpenses([]);
      return;
    }

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/groups/${targetGroupId}/expenses`,
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      if (!response.ok) {
        setExpenses([]);
        return;
      }

      const data = await response.json();

      setExpenses(data);
    } catch (error) {
      console.error("Expenses error:", error);
      setExpenses([]);
    }
  };

  const loadPaymentHistory = async (groupId: number) => {
    const token = localStorage.getItem("token");

    setPaymentHistoryLoading(true);
    setPaymentHistoryError("");

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/payments/history/${groupId}`,
        {
          headers: {
            Authorization: `Bearer ${token}`
          },
        }
      );

      if (!response.ok) {
        throw new Error(`Payment history failed (${response.status})`);
      }

      const data: PaymentHistoryItem[] = await response.json();
      setPaymentHistory(data);
    } catch (error) {
      setPaymentHistoryError(
        error instanceof Error
          ? error.message
          : "Unable to load payment history"
      );
    } finally {
      setPaymentHistoryLoading(false);
    }
  };

  const loadEqualSplitMembers = async (groupId: number) => {
    const token = localStorage.getItem("token");
    setEqualSplitMembersLoading(true);
    setEqualSplitMembersError("");
    setEqualSplitMembers([]);
    setEqualSplitPayerId("");
    try {
      const response = await fetch(
        `${API_BASE_URL}/api/groups/${groupId}/members`,
        { headers: { Authorization: `Bearer ${token}` } }
      );
      if (!response.ok) {
        throw new Error(`Group members request failed (${response.status})`);
      }
      const data: GroupMember[] = (await response.json() as GroupMember[]).sort(
        (first, second) => first.user.id - second.user.id
      );
      setEqualSplitMembers(data);
      const currentUser = data.find(
        (member) => member.user.email.toLowerCase() === email.toLowerCase()
      );
      if (currentUser) {
        setEqualSplitPayerId(String(currentUser.user.id));
      } else if (data.length > 0) {
        setEqualSplitPayerId(String(data[0].user.id));
      }
    } catch (error) {
      console.error("Equal split members error:", error);
      setEqualSplitMembersError(
        error instanceof Error ? error.message : "Unable to load group members"
      );
    } finally {
      setEqualSplitMembersLoading(false);
    }
  };

  // =========================
  // LOAD PERSONAL EXPENSES
  // =========================

  const loadPersonalExpenses = async () => {
    const token = localStorage.getItem("token");
    if (!token) {
      setPersonalExpenses([]);
      return;
    }

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/personal/expenses`,
        { headers: { Authorization: `Bearer ${token}` } }
      );
      if (!response.ok) {
        const details = await response.text();
        console.error("Personal expenses request failed:", response.status, details);
        setPersonalExpenses([]);
        return;
      }
      const data = await response.json();
      setPersonalExpenses(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error("Personal expenses error:", error);
      setPersonalExpenses([]);
    }
  };

  // =========================
  // LOAD ALL GROUP DATA
  // =========================

  const loadGroupDashboardData = async (groupId: number) => {
    setActiveGroupId(groupId);

    await Promise.all([
      loadMembers(groupId),
      loadExpenses(groupId),
      loadBalances(groupId),
      loadReminders(groupId),
      loadSummary(groupId),
    ]);
  };

  // =========================
  // LOGIN
  // =========================

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();

    const trimmedEmail = email.trim();

    if (!trimmedEmail || !password) {
      alert("Please enter both email and password");
      return;
    }

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/users/login`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            email: trimmedEmail,
            password,
          }),
        }
      );

      const responseText = await response.text();
      let data: { message?: string; error?: string; token?: string } | null = null;

      if (responseText) {
        try {
          data = JSON.parse(responseText) as {
            message?: string;
            error?: string;
            token?: string;
          };
        } catch {
          data = null;
        }
      }

      if (!response.ok) {
        alert(data?.message || data?.error || responseText || "Login failed");
        return;
      }

      if (data?.token) {
        localStorage.setItem("token", data.token);
      }

      setLoggedIn(true);
      await loadPersonalExpenses();

      // Load all groups first.
      const loadedGroups = await loadGroups();

      // Prefer the existing Roommates group (Group 2)
      // so previously created expenses/splits are restored.
      if (loadedGroups.length > 0) {
        const initialGroup =
          loadedGroups.find((group) => group.id === 2) ??
          loadedGroups[0];

        const initialGroupId = initialGroup.id;

        // Remember which group belongs to the dashboard.
        setDashboardGroupId(initialGroupId);

        await loadGroupDashboardData(initialGroupId);
      } else {
        setActiveGroupId(null);
        setDashboardGroupId(null);
        setMembers([]);
        setExpenses([]);
        setBalances([]);
        setReminders([]);
        setSummary(null);
      }
    } catch (error) {
      console.error("Login error:", error);
      alert("Cannot connect to backend");
    }
  };

  // =========================
  // SIGNUP
  // =========================

  const handleSignup = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!signupName.trim()) {
      alert("Please enter your name");
      return;
    }

    if (!signupEmail.trim()) {
      alert("Please enter your email");
      return;
    }

    if (signupPassword.length < 6) {
      alert("Password must be at least 6 characters");
      return;
    }

    if (signupPassword !== confirmPassword) {
      alert("Passwords do not match");
      return;
    }

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/users/register`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            name: signupName.trim(),
            email: signupEmail.trim(),
            password: signupPassword,
          }),
        }
      );

      const responseText = await response.text();

      console.log("========== SIGNUP RESPONSE ==========");
      console.log("Status:", response.status);
      console.log("Status Text:", response.statusText);
      console.log("Response:", responseText);
      console.log("======================================");

      if (!response.ok) {
        let errorMessage = responseText;

        try {
          const errorData = JSON.parse(responseText);

          errorMessage =
            errorData.message ||
            errorData.error ||
            responseText;
        } catch {
          // Response was not JSON
        }

        if (response.status === 403) {
          alert(
            "Registration blocked by backend.\n\n" +
              "HTTP Status: 403\n\n" +
              "Please check SecurityConfig and make sure /api/users/register is permitted."
          );
        } else if (response.status === 409) {
          alert(
            "This email is already registered.\n\n" +
              "Please use another email."
          );
        } else if (response.status === 400) {
          alert(
            "Invalid registration request.\n\n" +
              errorMessage
          );
        } else if (response.status === 500) {
          alert(
            "Server error while creating the account.\n\n" +
              errorMessage
          );
        } else {
          alert(
            "Registration failed.\n\n" +
              `HTTP Status: ${response.status}\n\n` +
              errorMessage
          );
        }

        return;
      }

      alert(
        "Account created successfully! 🎉\n\nPlease login with your new account."
      );

      setShowSignup(false);
      setEmail(signupEmail.trim());
      setPassword(signupPassword);

      setSignupName("");
      setSignupEmail("");
      setSignupPassword("");
      setConfirmPassword("");
    } catch (error) {
      console.error("Signup connection error:", error);

      alert(
        "Cannot connect to backend.\n\n" +
          "Make sure Spring Boot is running on port 8081."
      );
    }
  };

  // =========================
  // CREATE GROUP
  // =========================

  const createGroup = async (e: React.FormEvent) => {
    e.preventDefault();

    const token = localStorage.getItem("token");

    if (!newGroupName.trim()) {
      alert("Please enter a group name");
      return;
    }

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/groups`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({
            name: newGroupName.trim(),
          }),
        }
      );

      const data = await response.json();

      if (!response.ok) {
        alert("Failed to create group");
        return;
      }

      alert(
        `Group "${data.name}" created successfully! 🎉`
      );

      setNewGroupName("");
      setShowCreateGroup(false);

      const updatedGroups = await loadGroups();

      if (updatedGroups.length > 0) {
        const newGroup = updatedGroups.find(
          (group: Group) => group.id === data.id
        );

        if (newGroup) {
          // The newly created group becomes the dashboard group.
          setDashboardGroupId(newGroup.id);

          await loadGroupDashboardData(newGroup.id);
        }
      }
    } catch (error) {
      console.error("Create group error:", error);
      alert("Cannot connect to backend");
    }
  };

  // =========================
  // RENAME GROUP
  // =========================

  const renameGroup = async (
    groupId: number,
    currentName: string
  ) => {
    const newName = window.prompt(
      "Enter new group name:",
      currentName || ""
    );

    if (newName === null) {
      return;
    }

    if (!newName.trim()) {
      alert("Please enter a group name");
      return;
    }

    const token = localStorage.getItem("token");

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/groups/${groupId}`,
        {
          method: "PUT",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({
            name: newName.trim(),
          }),
        }
      );

      if (!response.ok) {
        alert("Failed to rename group");
        return;
      }

      alert("Group renamed successfully!");

      await loadGroups();

      if (activeGroupId === groupId) {
        await loadSummary(groupId);
      }
    } catch (error) {
      console.error("Rename group error:", error);
      alert("Cannot connect to backend");
    }
  };

  // =========================
  // OPEN GROUP PAGE
  // =========================

  const openGroup = async (groupId: number) => {
    // Remember the dashboard group before switching
    // to the selected group's page.
    if (activeGroupId !== null) {
      setDashboardGroupId(activeGroupId);
    }

    setSelectedGroup(groupId);
    setActiveGroupId(groupId);
    setShowGroupPage(true);

    setShowAddMember(false);

    setShowExpenses(false);
    setShowAddExpense(false);
    setShowSplitForm(false);
    setShowEqualSplitWizard(false);
    setShowTransactionHistory(false);

    setSelectedExpense(null);
    setSplits([]);

    await loadGroupDashboardData(groupId);
  };

  // =========================
  // BACK TO DASHBOARD
  // =========================

  const backToDashboard = async () => {
    setShowGroupPage(false);
    setSelectedGroup(null);

    setShowAddMember(false);

    setNewMemberName("");
    setNewMemberEmail("");

    setShowExpenses(false);
    setShowAddExpense(false);
    setShowSplitForm(false);
    setShowEqualSplitWizard(false);
    setShowTransactionHistory(false);

    setSelectedExpense(null);
    setSplits([]);

    // Restore the group that was active on the dashboard
    // before entering the group page.
    if (dashboardGroupId !== null) {
      await loadGroupDashboardData(dashboardGroupId);
    } else if (activeGroupId !== null) {
      // Safety fallback in case dashboardGroupId has not been set.
      setDashboardGroupId(activeGroupId);
      await loadGroupDashboardData(activeGroupId);
    }
  };

  // =========================
  // ADD MEMBER
  // =========================

  const addMember = async (e: React.FormEvent) => {
    e.preventDefault();

    const token = localStorage.getItem("token");

    if (!selectedGroup) {
      alert("Please select a group");
      return;
    }

    if (!newMemberName.trim()) {
      alert("Please enter the member name");
      return;
    }

    if (!newMemberEmail.trim()) {
      alert("Please enter the member email");
      return;
    }

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/groups/${selectedGroup}/members`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({
            email: newMemberEmail.trim(),
          }),
        }
      );

      const responseText = await response.text();

      let data: any = {};

      try {
        data = JSON.parse(responseText);
      } catch {
        // Backend response was not JSON
      }

      if (!response.ok) {
        if (response.status === 409) {
          alert(
            "This person is already a member of this group."
          );
        } else if (response.status === 404) {
          alert(
            "User not found.\n\nAsk the person to register first."
          );
        } else {
          alert(
            data.message ||
              data.error ||
              "Failed to add member"
          );
        }

        return;
      }

      alert(
        `${newMemberName.trim()} added successfully! 🎉`
      );

      setNewMemberName("");
      setNewMemberEmail("");
      setShowAddMember(false);

      await loadMembers(selectedGroup);

      if (activeGroupId === selectedGroup) {
        await loadGroupDashboardData(selectedGroup);
      }
    } catch (error) {
      console.error("Add member error:", error);
      alert("Cannot connect to backend");
    }
  };

  // =========================
  // LOAD SPLITS
  // =========================

  const loadSplits = async (expenseId: number) => {
    const token = localStorage.getItem("token");

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/expenses/${expenseId}/splits`,
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      if (!response.ok) {
        setSplits([]);
        return;
      }

      const data = await response.json();
      setSplits(data);
    } catch (error) {
      console.error("Load splits error:", error);
      setSplits([]);
    }
  };

  // =========================
  // OPEN SPLIT
  // =========================

  const openSplitExpense = async (expense: Expense) => {
    setSelectedExpense(expense);
    setShowSplitForm(true);

    setShowGroupPage(false);
    setSelectedGroup(null);

    setShowExpenses(false);
    setShowAddExpense(false);
    setShowAddMember(false);

    setSplits([]);

    await loadSplits(expense.id);

    if (activeGroupId) {
      await loadMembers(activeGroupId);
    }
  };

  // =========================
  // CREATE EXPENSE
  // =========================

  const createExpense = async (e: React.FormEvent) => {
    e.preventDefault();

    const token = localStorage.getItem("token");

    if (!activeGroupId) {
      alert("Please select a group first");
      return;
    }

    if (!expenseDescription.trim()) {
      alert("Please enter an expense description");
      return;
    }

    if (!expenseAmount || Number(expenseAmount) <= 0) {
      alert("Please enter a valid amount");
      return;
    }

    if (!paidByUserId) {
      alert("Please select who paid");
      return;
    }

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/groups/${activeGroupId}/expenses`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({
            description: expenseDescription,
            amount: Number(expenseAmount),
            paidByUserId: Number(paidByUserId),
          }),
        }
      );

      const data = await response.json();

      if (!response.ok) {
        console.error(data);
        alert("Failed to create expense");
        return;
      }

      alert("Expense created successfully!");

      setExpenseDescription("");
      setExpenseAmount("");
      setShowAddExpense(false);

      await loadExpenses(activeGroupId);
      await loadBalances(activeGroupId);
      await loadSummary(activeGroupId);
      await loadReminders(activeGroupId);
    } catch (error) {
      console.error(error);
      alert("Cannot connect to backend");
    }
  };

  // =========================
  // CREATE PERSONAL EXPENSE
  // =========================

  const createPersonalExpense = async (e: React.FormEvent) => {
    e.preventDefault();
    const token = localStorage.getItem("token");

    if (!personalExpenseDescription.trim()) {
      alert("Please enter an expense description");
      return;
    }
    const amount = Number(personalExpenseAmount);
    if (!Number.isFinite(amount) || amount <= 0) {
      alert("Please enter a valid amount");
      return;
    }

    try {
      const response = await fetch(`${API_BASE_URL}/api/personal/expenses`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          description: personalExpenseDescription.trim(),
          amount,
          paidByUserId: null,
        }),
      });
      const responseText = await response.text();
      if (!response.ok) {
        console.error("Create personal expense failed:", response.status, responseText);
        alert(`Failed to create personal expense (${response.status}). Check the backend terminal.`);
        return;
      }
      setPersonalExpenseDescription("");
      setPersonalExpenseAmount("");
      setShowPersonalExpenseForm(false);
      await loadPersonalExpenses();
    } catch (error) {
      console.error("Create personal expense error:", error);
      alert("Cannot connect to backend");
    }
  };

  const reviewEqualSplit = () => {
    const amount = Number(equalSplitAmount);
    if (!equalSplitGroupId) {
      alert("Please select a group");
      return;
    }
    if (!equalSplitExpenseId || !expenses.some((expense) => expense.id === equalSplitExpenseId)) {
      alert("Please select an existing expense");
      return;
    }
    if (!Number.isFinite(amount) || amount <= 0
        || Math.abs(amount * 100 - Math.round(amount * 100)) > 0.000001) {
      alert("Enter a positive amount with at most two decimal places");
      return;
    }
    if (equalSplitMembers.length === 0 || !equalSplitPayerId) {
      alert("Select a group with members and a payer");
      return;
    }
    // Start the review with an equal allocation; the user can then edit any member.
    const totalPaise = Math.round(amount * 100);
    const basePaise = Math.floor(totalPaise / equalSplitMembers.length);
    const remainderPaise = totalPaise % equalSplitMembers.length;
    const initialShares: Record<number, number> = {};
    equalSplitMembers.forEach((member, index) => {
      initialShares[member.user.id] = basePaise + (index < remainderPaise ? 1 : 0);
    });
    setEqualSplitSharesPaise(initialShares);
    setEqualSplitReview(true);
  };

  const submitEqualSplitExpense = async () => {
    const token = localStorage.getItem("token");
    if (!equalSplitGroupId || equalSplitSubmitting) {
      return;
    }
    setEqualSplitSubmitting(true);
    try {
      const expense = expenses.find((item) => item.id === equalSplitExpenseId);
      if (!expense) {
        throw new Error("Please select an existing expense");
      }
      const totalPaise = Math.round(expense.amount * 100);
      const requests = equalSplitMembers.map((member) => ({
        userId: member.user.id,
        amount: (equalSplitSharesPaise[member.user.id] ?? 0) / 100,
      }));
      const allocatedPaise = requests.reduce(
        (sum, request) => sum + Math.round(request.amount * 100),
        0
      );
      if (requests.some((request) => !Number.isFinite(request.amount) || request.amount < 0)
          || allocatedPaise !== totalPaise) {
        throw new Error(`The participant amounts must be non-negative and add up to ₹${(totalPaise / 100).toFixed(2)}.`);
      }
      const response = await fetch(
        `${API_BASE_URL}/api/expenses/${expense.id}/splits`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify(requests),
        }
      );
      const body = await response.text();
      if (!response.ok) {
        throw new Error(body || `Expense split failed (${response.status})`);
      }
      setActiveGroupId(equalSplitGroupId);
      setDashboardGroupId(equalSplitGroupId);
      setShowEqualSplitWizard(false);
      setShowExpenses(true);
      setEqualSplitReview(false);
            setEqualSplitDescription("");
      setEqualSplitAmount("");
      setEqualSplitExpenseId("");
      setEqualSplitSharesPaise({});
      setEqualSplitSubmissionKey(crypto.randomUUID());
      await loadGroupDashboardData(equalSplitGroupId);
    } catch (error) {
      console.error("Create equal split expense error:", error);
      alert(error instanceof Error ? error.message : "Unable to create expense");
    } finally {
      setEqualSplitSubmitting(false);
    }
  };

  // =========================
  // CREATE EQUAL SPLITS
  // =========================

  const createEqualSplits = async () => {
    if (!selectedExpense) {
      alert("Please select an expense");
      return;
    }

    if (splits.length > 0) {
      alert("This expense already has split details.");
      return;
    }

    if (members.length === 0) {
      alert("No group members found");
      return;
    }

    const token = localStorage.getItem("token");

    const totalPaise = Math.round(
      selectedExpense.amount * 100
    );

    const memberCount = members.length;

    const baseShare = Math.floor(
      totalPaise / memberCount
    );

    const remainder = totalPaise % memberCount;

    const requests = members.map((member, index) => {
      let memberShare = baseShare;

      if (index < remainder) {
        memberShare += 1;
      }

      return {
        userId: member.user.id,
        amount: memberShare / 100,
      };
    });

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/expenses/${selectedExpense.id}/splits`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify(requests),
        }
      );

      const data = await response.json();

      if (!response.ok) {
        alert("Failed to create expense splits");
        return;
      }

      setSplits(data);

      alert("Expense split equally!");

      if (activeGroupId) {
        await loadBalances(activeGroupId);
        await loadReminders(activeGroupId);
        await loadSummary(activeGroupId);
      }

      await loadSplits(selectedExpense.id);
    } catch (error) {
      console.error(error);
      alert("Cannot connect to backend");
    }
  };

  // =========================
// RAZORPAY PAY NOW
// =========================

const recoverPayment = async (splitId: number) => {
  const token = localStorage.getItem("token");
  try {
    const response = await fetch(
      `${API_BASE_URL}/api/payments/${splitId}/status`,
      { headers: { Authorization: `Bearer ${token}` } }
    );
    const data = await response.json();
    if (!response.ok || data.success !== true) {
      alert(data.message || "Unable to check payment status");
      return;
    }
    if (data.paid !== true) {
      alert("Razorpay has not confirmed a captured payment yet.");
      return;
    }

    setSplits((currentSplits) =>
      currentSplits.map((split) =>
        split.id === splitId ? { ...split, paid: true } : split
      )
    );
    if (activeGroupId) {
      await loadBalances(activeGroupId);
      await loadReminders(activeGroupId);
      await loadSummary(activeGroupId);
    }
    if (selectedExpense) {
      await loadSplits(selectedExpense.id);
    }
  } catch (error) {
    console.error("Payment status error:", error);
    alert("Unable to confirm payment status. The split remains pending.");
  }
};

const payNow = async (splitId: number) => {
  const token = localStorage.getItem("token");

  try {
    // 1. Create Razorpay order from backend
    const response = await fetch(
      `${API_BASE_URL}/api/payments/create-order`,
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          splitId: splitId,
        }),
      }
    );

    const data = await response.json();

    if (!response.ok) {
      alert("Failed to create payment order");
      return;
    }

    // 2. Open Razorpay Checkout
    const options = {
      key: data.keyId,
      amount: data.amount,
      currency: data.currency,
      name: "Expense Sharing App",
      description: "Expense Split Payment",
      order_id: data.orderId,

      handler: async function (paymentResponse: any) {
  try {
    const verifyResponse = await fetch(
      `${API_BASE_URL}/api/payments/verify`,
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          splitId: splitId,
          razorpayOrderId: paymentResponse.razorpay_order_id,
          razorpayPaymentId: paymentResponse.razorpay_payment_id,
          razorpaySignature: paymentResponse.razorpay_signature,
        }),
      }
    );

    const verifyData = await verifyResponse.json();

    if (!verifyResponse.ok || verifyData.success !== true
        || verifyData.paid !== true) {
      alert("Payment verification failed");
      console.error("Verification error:", verifyData);
      return;
    }

    alert("Payment successful and verified!");
    if (showTransactionHistory && activeGroupId !== null) {
      await loadPaymentHistory(activeGroupId);}
    setSplits((currentSplits) =>
      currentSplits.map((split) =>
        split.id === splitId
          ? { ...split, paid: true }
          : split
      )
    );

    if (activeGroupId) {
      await loadBalances(activeGroupId);
      await loadReminders(activeGroupId);
      await loadSummary(activeGroupId);
    }

    if (selectedExpense) {
      await loadSplits(selectedExpense.id);
    }

  } catch (error) {
    console.error("Payment verification error:", error);
    alert(
      "Payment verification is pending. Use Check Payment Status to securely retry."
    );
  }
},

      theme: {
        color: "#3399cc",
      },
    };

    const Razorpay = (window as WindowWithRazorpay).Razorpay;
    if (!Razorpay) {
      alert("Razorpay Checkout could not be loaded");
      return;
    }
    const razorpay = new Razorpay(options as RazorpayCheckoutOptions);
    razorpay.on("payment.failed", () => {
      alert("Payment failed. The split remains pending.");
    });
    razorpay.open();

  } catch (error) {
    console.error(error);
    alert("Cannot connect to payment service");
  }
};

  // =========================
  // DYNAMIC EXPENSE ANALYTICS
  // =========================

  const chartColors = [
    "#4f8cff",
    "#8b5cf6",
    "#10b981",
    "#f59e0b",
    "#ef4444",
    "#06b6d4",
    "#ec4899",
    "#84cc16",
  ];

  // Combine group and personal expenses
  const chartExpenses = [
    ...expenses.map((expense) => ({
      ...expense,
      chartSource: "Group" as const,
    })),
    ...personalExpenses.map((expense) => ({
      ...expense,
      chartSource: "Personal" as const,
    })),
  ].filter(
    (expense) =>
      Number.isFinite(Number(expense.amount)) &&
      Number(expense.amount) > 0
  );

  // Combined total
  const chartTotal = chartExpenses.reduce(
    (total, expense) => total + Number(expense.amount),
    0
  );

  // Generate pie chart
  let currentDegree = 0;

  const chartGradient =
    chartTotal === 0
      ? "#e5e7eb 0deg 360deg"
      : `conic-gradient(${chartExpenses
          .map((expense, index) => {
            const start = currentDegree;

            currentDegree +=
              (Number(expense.amount) / chartTotal) * 360;

            return `${chartColors[index % chartColors.length]} ${start}deg ${currentDegree}deg`;
          })
          .join(", ")})`;

  const equalSplitTotalPaise = Math.round(
    Number(equalSplitAmount) * 100
  );

  const equalSplitRemainderPaise =
    equalSplitMembers.length > 0
      ? equalSplitTotalPaise % equalSplitMembers.length
      : 0;

  // =========================
  // LOGOUT
  // =========================

  const handleLogout = () => {
    localStorage.removeItem("token");

    setLoggedIn(false);

    setBalances([]);
    setMembers([]);
    setExpenses([]);
    setPersonalExpenses([]);
    setShowPersonalExpenseForm(false);
    setPersonalExpenseDescription("");
    setPersonalExpenseAmount("");
    setSplits([]);
    setReminders([]);
    setSummary(null);
    setGroups([]);

    setActiveGroupId(null);
    setDashboardGroupId(null);

    setShowCreateGroup(false);
    setNewGroupName("");

    setShowGroupPage(false);
    setSelectedGroup(null);

    setShowAddMember(false);
    setNewMemberName("");
    setNewMemberEmail("");

    setShowExpenses(false);
    setShowAddExpense(false);
    setShowSplitForm(false);
    setShowEqualSplitWizard(false);
    setShowTransactionHistory(false);

    setSelectedExpense(null);

    setExpenseDescription("");
    setExpenseAmount("");

    setPaidByUserId("3");

    setEmail("");
    setPassword("");

    setShowSignup(false);
    setSignupName("");
    setSignupEmail("");
    setSignupPassword("");
    setConfirmPassword("");
  };

  // ============================================================
  // LOGIN / SIGNUP PAGE
  // ============================================================

  if (!loggedIn) {
    return (
      <div className="login-page">

        <div className="login-glow glow-one" />
        <div className="login-glow glow-two" />

        <div className="login-card">

          <div className="brand-icon">
            ₹
          </div>

          <h1>Expense Sharing</h1>

          <p className="subtitle">
            Smartly manage shared expenses
          </p>

          {!showSignup ? (

            <>
              <form onSubmit={handleLogin}>

                <label>Email</label>

                <input
                  type="email"
                  placeholder="Enter your email"
                  value={email}
                  onChange={(e) =>
                    setEmail(e.target.value)
                  }
                  required
                />

                <label>Password</label>

                <input
                  type="password"
                  placeholder="Enter your password"
                  value={password}
                  onChange={(e) =>
                    setPassword(e.target.value)
                  }
                  required
                />

                <button
                  type="submit"
                  className="primary-button login-button"
                >
                  Login →
                </button>

              </form>

              <p className="register-text">
                Don't have an account?{" "}

                <button
                  type="button"
                  className="link-button"
                  onClick={() => {
                    setShowSignup(true);
                    setEmail("");
                    setPassword("");
                  }}
                >
                  Sign Up
                </button>
              </p>
            </>

          ) : (

            <>
              <form onSubmit={handleSignup}>

                <label>Name</label>

                <input
                  type="text"
                  placeholder="Enter your full name"
                  value={signupName}
                  onChange={(e) =>
                    setSignupName(e.target.value)
                  }
                  required
                />

                <label>Email</label>

                <input
                  type="email"
                  placeholder="Enter your email"
                  value={signupEmail}
                  onChange={(e) =>
                    setSignupEmail(e.target.value)
                  }
                  required
                />

                <label>Password</label>

                <input
                  type="password"
                  placeholder="Create a password"
                  value={signupPassword}
                  onChange={(e) =>
                    setSignupPassword(e.target.value)
                  }
                  required
                />

                <label>Confirm Password</label>

                <input
                  type="password"
                  placeholder="Confirm your password"
                  value={confirmPassword}
                  onChange={(e) =>
                    setConfirmPassword(e.target.value)
                  }
                  required
                />

                <button
                  type="submit"
                  className="primary-button login-button"
                >
                  Create Account →
                </button>

              </form>

              <p className="register-text">
                Already have an account?{" "}

                <button
                  type="button"
                  className="link-button"
                  onClick={() => {
                    setShowSignup(false);

                    setSignupName("");
                    setSignupEmail("");
                    setSignupPassword("");
                    setConfirmPassword("");
                  }}
                >
                  Login
                </button>
              </p>
            </>

          )}

        </div>
      </div>
    );
  }

  // ============================================================
  // GROUP PAGE
  // ============================================================

  if (showGroupPage && selectedGroup) {
    const currentGroup = groups.find(
      (group) => group.id === selectedGroup
    );

    const currentGroupName =
      currentGroup?.name || "Unnamed Group";

    return (
      <div className="dashboard-page">

        <nav className="navbar">

          <div className="navbar-brand">

            <div className="navbar-logo">
              ₹
            </div>

            <div>

              <div className="navbar-title">
                Expense Sharing
              </div>

              <div className="navbar-subtitle">
                Group Management
              </div>

            </div>

          </div>

          <div className="navbar-right">

            <div className="user-pill">
              <span className="user-dot" />
              {email}
            </div>

            <button
              className="logout-button"
              onClick={handleLogout}
            >
              Logout
            </button>

          </div>

        </nav>

        <main className="dashboard-container">

          <div className="dashboard-header">

            <div>

              <button
                type="button"
                className="secondary-button"
                onClick={backToDashboard}
              >
                ← Back to Dashboard
              </button>

              <p
                className="eyebrow"
                style={{ marginTop: "25px" }}
              >
                GROUP
              </p>

              <h1>
                {currentGroupName}
              </h1>

              <p className="welcome">
                Manage your group members and shared
                expenses.
              </p>

            </div>

            <div className="group-badge">
              <span>●</span>
              Group {selectedGroup}
            </div>

          </div>

          {/* GROUP INFORMATION */}

          <div className="dashboard-card">

            <div className="card-heading">

              <div>

                <p className="card-label">
                  GROUP DETAILS
                </p>

                <h2>
                  {currentGroupName}
                </h2>

                <p className="group-id-text">
                  Group ID: {selectedGroup}
                </p>

              </div>

              <div className="card-icon blue">
                👥
              </div>

            </div>

            <div className="group-member-actions">

              <div>

                <strong>
                  {members.length}{" "}
                  {members.length === 1
                    ? "Member"
                    : "Members"}
                </strong>

                <p className="form-hint">
                  People currently added to this group.
                </p>

              </div>

              <button
                type="button"
                className="primary-button"
                onClick={() => {
                  setShowAddMember(true);
                }}
              >
                ＋ Add Members
              </button>

            </div>

            {/* ADD MEMBER FORM */}

            {showAddMember && (

              <div className="add-member-panel">

                <div className="card-heading">

                  <div>

                    <p className="card-label">
                      NEW MEMBER
                    </p>

                    <h2>
                      Add Member
                    </h2>

                  </div>

                </div>

                <form onSubmit={addMember}>

                  <label>
                    Name
                  </label>

                  <input
                    type="text"
                    placeholder="Enter member name"
                    value={newMemberName}
                    onChange={(e) =>
                      setNewMemberName(
                        e.target.value
                      )
                    }
                    required
                  />

                  <label>
                    Email
                  </label>

                  <input
                    type="email"
                    placeholder="Enter registered email"
                    value={newMemberEmail}
                    onChange={(e) =>
                      setNewMemberEmail(
                        e.target.value
                      )
                    }
                    required
                  />

                  <p className="form-hint">
                    The person must already have an
                    account.
                  </p>

                  <div className="form-actions">

                    <button
                      type="submit"
                      className="primary-button"
                    >
                      Add Member
                    </button>

                    <button
                      type="button"
                      className="secondary-button"
                      onClick={() => {
                        setShowAddMember(false);
                        setNewMemberName("");
                        setNewMemberEmail("");
                      }}
                    >
                      Cancel
                    </button>

                  </div>

                </form>

              </div>
            )}

          </div>

          {/* MEMBERS */}

          <div className="dashboard-card">

            <div className="card-heading">

              <div>

                <p className="card-label">
                  MEMBERS
                </p>

                <h2>
                  Group Members
                </h2>

              </div>

              <span className="count-badge">
                {members.length}
              </span>

            </div>

            {members.length === 0 ? (

              <div className="empty-state">

                <div>
                  👥
                </div>

                <p>
                  No members found.
                </p>

                <small>
                  Click "Add Members" to add someone.
                </small>

              </div>

            ) : (

              <div className="member-grid">

                {members.map(
                  (member) => (

                    <div
                      className="member-box modern-member"
                      key={member.id}
                    >

                      <div className="member-avatar">

                        {member.user.name
                          .charAt(0)
                          .toUpperCase()}

                      </div>

                      <div>

                        <h3>
                          {member.user.name}
                        </h3>

                        <p>
                          {member.user.email}
                        </p>

                        <small>
                          User ID:{" "}
                          {member.user.id}
                        </small>

                      </div>

                    </div>
                  )
                )}

              </div>
            )}

          </div>

        </main>

      </div>
    );
  }

  // ============================================================
  // NORMAL DASHBOARD
  // ============================================================

  const activeGroup = groups.find(
    (group) => group.id === activeGroupId
  );

  const activeGroupName =
    activeGroup?.name || "No Group Selected";

  const currentUserId = members.find(
    (member) =>
      member.user.email.toLowerCase() === email.toLowerCase()
  )?.user.id;

  // Show pending balances using account-wide reminders
const myBalances: Balance[] = reminders.map((reminder) => ({
  fromUser: reminder.fromUser,
  toUser: reminder.toUser,
  amount: reminder.amount,
  paid: false,
}));

const myPendingBalanceCount = myBalances.length;


  return (
    <div className="dashboard-page">

      <nav className="navbar">

        <div className="navbar-brand">

          <div className="navbar-logo">
            ₹
          </div>

          <div>

            <div className="navbar-title">
              Expense Sharing
            </div>

            <div className="navbar-subtitle">
              Smart expense management
            </div>

          </div>

        </div>

        <div className="navbar-right">

          <div className="user-pill">
            <span className="user-dot" />
            {email}
          </div>

          <button
            className="logout-button"
            onClick={handleLogout}
          >
            Logout
          </button>

        </div>

      </nav>

      <main className="dashboard-container">

        <div className="dashboard-header">

          <div>

            <p className="eyebrow">
              GROUP DASHBOARD
            </p>

            <h1>
              👋 Hi, Welcome back {members.find(
                (member) =>
                  member.user.email.trim().toLowerCase() ===
                  email.trim().toLowerCase()
              )?.user.name || "User"}!
            </h1>


            <p className="welcome">
              Here's what's happening with your
              shared expenses.
            </p>

          </div>

          <div className="group-badge">
            <span>●</span>
            {activeGroupName}
            {activeGroupId
              ? ` · Group ${activeGroupId}`
              : ""}
          </div>

        </div>

        {/* =========================
            SUMMARY CARDS
        ========================= */}

                <div className="summary-cards">

          {/* GROUP + PERSONAL TOTAL EXPENSES */}
          <div className="stat-card stat-blue">
            <div className="stat-icon">₹</div>
            <div>
              <p>Total Expenses</p>
              <h2>
                ₹{(
                  (summary?.totalExpenses ?? 0) +
                  personalExpenses.reduce(
                    (total, expense) =>
                      total + Number(expense.amount || 0),
                    0
                  )
                  ).toFixed(2)}
                  </h2>
                  <span>Group + personal expenses</span>
                  </div>
                  </div>


          {/* GROUP-WIDE TRANSACTIONS */}
          <button
            type="button"
            className="stat-card stat-purple stat-card-button"
            onClick={() => {
              const nextVisible = !showTransactionHistory;
              setShowTransactionHistory(nextVisible);
              if (nextVisible && activeGroupId !== null) {
                void loadPaymentHistory(activeGroupId);
              }
            }}
            aria-expanded={showTransactionHistory}
          >
            <div className="stat-icon">#</div>
            <div>
              <p>Total Transactions</p>
              <h2>{summary?.totalExpenseCount ?? 0}</h2>
              <span>View transaction history</span>
            </div>
          </button>

          {/* GROUP-WIDE PAID BALANCES */}
          <div className="stat-card stat-green">
            <div className="stat-icon">✓</div>
            <div>
              <p>Paid Balances</p>
              <h2>
                {balances.filter((balance) => balance.paid).length}
              </h2>
              <span>Settled payments</span>
            </div>
          </div>

          {/* GROUP-WIDE PENDING BALANCES */}
          <div className="stat-card stat-orange">
            <div className="stat-icon">!</div>
            <div>
              <p>Pending Balances</p>
              <h2>
                {myPendingBalanceCount}
              </h2>
              <span>Need attention</span>
            </div>
          </div>

          {/* PERSONAL EXPENSE SHARE */}
          <div className="stat-card stat-purple">
            <div className="stat-icon">↗</div>
            <div>
              <p>My Share</p>
              <h2>
                ₹{(summary?.myShare ?? 0).toFixed(2)}
              </h2>
              <span>Your allocated expenses</span>
            </div>
          </div>

        </div>

        {showTransactionHistory && (
          <div className="dashboard-card transaction-history">
            <div className="card-heading">
              <div>
                <p className="card-label">TRANSACTIONS</p>
                <h2>Transaction History</h2>
              </div>
              <button
                type="button"
                className="secondary-button"
                onClick={() => setShowTransactionHistory(false)}
              >
                Close
              </button>
            </div>

            {paymentHistoryLoading ? (
              <p role="status">Loading transaction history…</p>
            ) : paymentHistoryError ? (
              <div className="empty-state transaction-error" role="alert">
                <p>{paymentHistoryError}</p>
                {activeGroupId !== null && (
                  <button
                    type="button"
                    className="secondary-button"
                    onClick={() => void loadPaymentHistory(activeGroupId)}
                  >
                    Retry
                  </button>
                )}
              </div>
            ) : activeGroupId === null ? (
              <div className="empty-state">
                <p>Please select a group to view payment history.</p>
              </div>
            ) : paymentHistory.length === 0 ? (
              <div className="empty-state">
                <p>No personal payments found for this group yet.</p>
              </div>
            ) : (
              <div className="transaction-list">
                {paymentHistory.map((payment) => (
                  <article className="transaction-row" key={payment.id}>
                    <div className="expense-icon">₹</div>

                    <div className="transaction-main">
                      <h3>{payment.description}</h3>
                      <p>
                        {payment.group} · Bill originally paid by {payment.paidTo}
                      </p>
                      <small>
                        {payment.paidAt
                          ? new Date(payment.paidAt).toLocaleString()
                          : "Payment date not recorded"}
                      </small>

                      {payment.paymentId && (
                        <div className="transaction-participants">
                          Razorpay reference: {payment.paymentId}
                        </div>
                      )}
                    </div>

                    <div className="transaction-side">
                      <strong>₹{payment.amount.toFixed(2)}</strong>
                      <span className="status paid">PAID</span>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </div>
        )}

        {/* =========================
            GROUP + BALANCES
        ========================= */}

        <div className="dashboard-grid">

          {/* GROUP CARD */}

          <div className="dashboard-card">

            <div className="card-heading">

              <div>

                <p className="card-label">
                  GROUP
                </p>

                <h2>
                  My Groups
                </h2>

              </div>

              <button
                type="button"
                className="card-icon blue"
                onClick={() => setShowCreateGroup(true)}
                title="Create New Group"
              >
                👥＋
              </button>

            </div>

            {/* CREATE GROUP FORM */}

            {showCreateGroup && (

              <div className="add-member-panel">

                <div className="card-heading">

                  <div>

                    <p className="card-label">
                      NEW GROUP
                    </p>

                    <h2>
                      Create Group
                    </h2>

                  </div>

                </div>

                <form onSubmit={createGroup}>

                  <label>
                    Group Name
                  </label>

                  <input
                    type="text"
                    placeholder="e.g. Friends, Family, Trip..."
                    value={newGroupName}
                    onChange={(e) =>
                      setNewGroupName(
                        e.target.value
                      )
                    }
                    required
                  />

                  <div className="form-actions">

                    <button
                      type="submit"
                      className="primary-button"
                    >
                      Create Group
                    </button>

                    <button
                      type="button"
                      className="secondary-button"
                      onClick={() => {
                        setShowCreateGroup(false);
                        setNewGroupName("");
                      }}
                    >
                      Cancel
                    </button>

                  </div>

                </form>

              </div>
            )}

            {/* EXISTING GROUPS */}

            {groups.length > 0 && (

              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  gap: "12px",
                  marginBottom: "20px",
                }}
              >

                {groups.map((group) => (

                  <div
                    className="group-box modern-group clickable-group"
                    key={group.id}
                    onClick={() =>
                      openGroup(group.id)
                    }
                  >

                    <div className="group-avatar">
                      {(group.name || "G")
                        .charAt(0)
                        .toUpperCase()}
                    </div>

                    <div>

                      <h3>
                        {group.name || "Unnamed Group"}
                      </h3>

                      <p>
                        Group ID: {group.id}
                      </p>

                    </div>

                    <span className="active-badge">
                      {activeGroupId === group.id
                        ? "Active"
                        : "Open"}
                    </span>

                    <button
                      type="button"
                      className="secondary-button"
                      onClick={(e) => {
                        e.stopPropagation();

                        renameGroup(
                          group.id,
                          group.name || ""
                        );
                      }}
                      title="Rename Group"
                    >
                      ✏️
                    </button>

                    <span className="group-open-arrow">
                      →
                    </span>

                  </div>

                ))}

              </div>
            )}

            {/* FALLBACK */}

            {groups.length === 0 && (

              <div className="empty-state">

                <div>
                  👥
                </div>

                <p>
                  No groups found.
                </p>

                <small>
                  Click 👥＋ to create a group.
                </small>

              </div>
            )}

          </div>

          {/* BALANCES */}

          <div className="dashboard-card">

            <div className="card-heading">

              <div>

                <p className="card-label">
                  MONEY FLOW
                </p>

                <h2>
                  Balances
                </h2>

              </div>

              <div className="card-icon purple">
                ⇄
              </div>

            </div>
            {myBalances.length === 0 ? (
              <div className="empty-state">
                <div style={{ fontSize: "48px" }}>
                  🎉
                  </div>
                  <h3 style={{ color: "#16a34a", margin: "12px 0 6px" }}>
                    You're All Settled!
                    </h3>
                    <p>
                      No pending balances. You're doing great!
                      </p>
                      <span style={{
                        display: "inline-block",
                        marginTop: "12px",
                        padding: "8px 16px",
                        borderRadius: "20px",
                        background: "#dcfce7",
                        color: "#166534",
                        fontWeight: 600
                        }}>
                          ✨ Debt-free for now
                          </span>
                          </div>
                          ) : (


              <div className="balance-list">

                {myBalances.map(
                  (balance, index) => (

                    <div
                      className="modern-balance"
                      key={index}
                    >

                      <div className="balance-person">

                        <div className="mini-avatar">
                          {balance.fromUser
                            .charAt(0)
                            .toUpperCase()}
                        </div>

                        <div>

                          <strong>
                            {balance.fromUser}
                          </strong>

                          <small>
                            owes{" "}
                            {balance.toUser}
                          </small>

                        </div>

                      </div>

                      <div className="balance-right">

                        <strong>
                          ₹
                          {balance.amount.toFixed(
                            2
                          )}
                        </strong>

                        <span
                          className={
                            balance.paid
                              ? "status paid"
                              : "status pending"
                          }
                        >
                          {balance.paid
                            ? "Paid"
                            : "Pending"}
                        </span>

                      </div>

                    </div>
                  )
                )}

              </div>
            )}

          </div>

        </div>

        {/* =========================
            ANALYTICS
        ========================= */}

        <div className="dashboard-grid analytics-grid">

          <div className="dashboard-card">

            <div className="card-heading">

              <div>

                <p className="card-label">
                  ANALYTICS
                </p>

                <h2>
                  Expense Overview
                </h2>

              </div>

              <div className="card-icon orange">
                ◔
              </div>

            </div>

            {expenses.length === 0 ? (

              <div className="empty-state">

                <div>
                  ◔
                </div>

                <p>
                  Add expenses to see analytics
                </p>

              </div>

            ) : (

              <div className="expense-chart">

                <div
                  className="pie-chart"
                  style={{
                    background: chartGradient,
                  }}
                >

                  <div className="pie-hole">

                    <strong>
                      ₹
                      {chartTotal.toFixed(2)}
                    </strong>

                    <span>
                      Total
                    </span>

                  </div>

                </div>

                <div className="chart-legend">

                  {chartExpenses.map((expense, index) => {
                      const percentage = chartTotal > 0
                        ? (Number(expense.amount) / chartTotal) * 100
                        : 0;
                      return (
                        <div
                          className="legend-item"
                          key={expense.id}
                        >

                          <span
                            className="legend-dot"
                            style={{ backgroundColor: chartColors[index % chartColors.length] }}
                          />

                          <div>

                            <strong>
                              {expense.description}
                            </strong>

                            <span>
                              ₹{Number(expense.amount).toFixed(2)} · {percentage.toFixed(1)}%
                            </span>

                          </div>

                        </div>
                      );
                    })}

                </div>

              </div>
            )}

          </div>

          <div className="dashboard-card">

            <div className="card-heading">

              <div>

                <p className="card-label">
                  GROUP INSIGHTS
                </p>

                <h2>
                  At a Glance
                </h2>

              </div>

              <div className="card-icon green">
                ✦
              </div>

            </div>

            <div className="insight-list">

              <div className="insight-row">

                <span>
                  Group
                </span>

                <strong>
                  {summary?.groupName ||
                    activeGroupName}
                </strong>

              </div>

              <div className="insight-row">

                <span>
                  Members
                </span>

                <strong>
                  {members.length || "—"}
                </strong>

              </div>

              <div className="insight-row">

                <span>
                  Expenses
                </span>

                <strong>
                  {summary?.totalExpenseCount ||
                    0}
                </strong>

              </div>

              <div className="insight-row">

                <span>
                  Total spent
                </span>

                <strong>
                  ₹
                  {summary
                    ? summary.totalExpenses.toFixed(
                        2
                      )
                    : "0.00"}
                </strong>

              </div>

              <div className="insight-row">

                <span>
                  Pending payments
                </span>

                <strong className="text-orange">

                  {
                    myPendingBalanceCount
                  }

                </strong>

              </div>

            </div>

          </div>

        </div>

        {/* =========================
            PAYMENT REMINDERS
        ========================= */}

        {reminders.length > 0 && (

          <div className="dashboard-card reminders-card">

            <div className="card-heading">

              <div>

                <p className="card-label">
                  ATTENTION
                </p>

                <h2>
                  🔔 Payment Reminders
                </h2>

              </div>

              <span className="notification-count">
                {reminders.length}
              </span>

            </div>

            <div className="reminder-grid">

              {reminders.map(
                (reminder) => (

                  <div
                    className="reminder-box"
                    key={reminder.splitId}
                  >

                    <div className="reminder-top">

                      <div className="warning-icon">
                        !
                      </div>

                      <span className="status pending">
                        Pending
                      </span>

                    </div>

                    <h3>
                      {reminder.message}
                    </h3>

                    <p>

                      <strong>
                        {reminder.fromUser}
                      </strong>{" "}

                      needs to pay{" "}

                      <strong>
                        {reminder.toUser}
                      </strong>

                    </p>

                    <div className="reminder-amount">

                      ₹
                      {reminder.amount.toFixed(
                        2
                      )}

                    </div>

                    <button
                      type="button"
                      className="primary-button"
                      onClick={() =>
                        payNow(
                          reminder.splitId
                        )
                      }
                    >
                      Pay Now
                    </button>

                  </div>

                )
              )}

            </div>

          </div>
        )}

        {/* =========================
            QUICK ACTIONS
        ========================= */}

        <div className="dashboard-card quick-actions-card">

          <div className="card-heading">

            <div>

              <p className="card-label">
                ACTION CENTER
              </p>

              <h2>
                Quick Actions
              </h2>

            </div>

          </div>

          <div className="action-buttons">

            {/* ADD EXPENSE */}

            <button
              className="action-button add"
              onClick={async () => {

                if (!activeGroupId) {
                  alert("Please select a group first");
                  return;
                }

                setShowAddExpense(true);
                setShowEqualSplitWizard(false);

                setShowExpenses(false);
                setShowSplitForm(false);

                setShowGroupPage(false);
                setSelectedGroup(null);

                setShowAddMember(false);

                setSelectedExpense(null);
                setSplits([]);

                await loadMembers(activeGroupId);

              }}
            >

              <span>
                ＋
              </span>

              Add Expense

            </button>

            <button
              type="button"
              className="action-button"
              onClick={() => {
                setShowEqualSplitWizard(true);
                setShowAddExpense(false);
                setShowExpenses(false);
                setShowSplitForm(false);
                setShowTransactionHistory(false);
                setEqualSplitDescription("");
                setEqualSplitAmount("");
                setEqualSplitExpenseId("");
                setEqualSplitReview(false);
                const groupId = activeGroupId ?? "";
                setEqualSplitGroupId(groupId);
                setEqualSplitMembers([]);
                setEqualSplitMembersError("");
                if (groupId) {
                  void loadEqualSplitMembers(groupId);
                  void loadExpenses(groupId);
                } else {
                  setExpenses([]);
                }
              }}
            >
              <span>÷</span>
              Split an Expense
            </button>

            {/* VIEW EXPENSES */}

            <button
              className="action-button view"
              onClick={async () => {

                if (!activeGroupId) {
                  alert("Please select a group first");
                  return;
                }

                setShowExpenses(true);
                setShowEqualSplitWizard(false);

                setShowAddExpense(false);
                setShowSplitForm(false);

                setShowGroupPage(false);
                setSelectedGroup(null);

                setShowAddMember(false);

                setSelectedExpense(null);
                setSplits([]);

                await loadExpenses(activeGroupId);

              }}
            >

              <span>
                ◫
              </span>

              View Expenses

            </button>

          </div>

        </div>

        <div className="dashboard-card personal-expenses-card">
          <div className="card-heading">
            <div>
              <p className="card-label">PRIVATE TO YOU</p>
              <h2>Personal Expenses</h2>
            </div>
            <button
              type="button"
              className="primary-button"
              onClick={() => {
                const opening = !showPersonalExpenseForm;
                setShowPersonalExpenseForm(opening);
                if (opening) {
                  void loadPersonalExpenses();
                }
              }}
            >
              {showPersonalExpenseForm ? "Close" : "＋ Add Personal Expense"}
            </button>
          </div>
          {showPersonalExpenseForm && (
            <form onSubmit={createPersonalExpense}>
              <label htmlFor="personal-expense-description">Description</label>
              <input
                id="personal-expense-description"
                type="text"
                value={personalExpenseDescription}
                onChange={(event) =>
                  setPersonalExpenseDescription(event.target.value)
                }
                placeholder="e.g. Personal groceries"
                required
              />
              <label htmlFor="personal-expense-amount">Amount</label>
              <input
                id="personal-expense-amount"
                type="number"
                value={personalExpenseAmount}
                onChange={(event) => setPersonalExpenseAmount(event.target.value)}
                min="0.01"
                step="0.01"
                required
              />
              <div className="form-actions">
                <button type="submit" className="primary-button">
                  Save Personal Expense
                </button>
              </div>
            </form>
          )}
          {personalExpenses.length === 0 ? (
            <div className="empty-state">
              <p>No personal expenses found.</p>
            </div>
          ) : (
            <div className="expense-list">
              {personalExpenses.map((expense) => (
                <div className="modern-expense" key={expense.id}>
                  <div className="expense-icon">₹</div>
                  <div className="expense-main">
                    <h3>{expense.description}</h3>
                    {expense.createdAt && (
                      <small>{new Date(expense.createdAt).toLocaleString()}</small>
                    )}
                  </div>
                  <div className="expense-side">
                    <strong>₹{expense.amount.toFixed(2)}</strong>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {showEqualSplitWizard && (
          <div className="dashboard-card form-card equal-split-wizard">
            <div className="card-heading">
              <div>
                <p className="card-label">GROUP EXPENSE</p>
                <h2>{equalSplitReview ? "Review Equal Split" : "Split an Expense"}</h2>
              </div>
            </div>
            {!equalSplitReview ? (
              <div>
                <label htmlFor="equal-split-group">Group</label>
                <select
                  id="equal-split-group"
                  value={equalSplitGroupId}
                  onChange={(event) => {
                    const groupId = event.target.value
                      ? Number(event.target.value)
                      : "";
                    setEqualSplitGroupId(groupId);
                    setEqualSplitMembers([]);
                    setEqualSplitMembersError("");
                    setEqualSplitReview(false);
                    setEqualSplitExpenseId("");
                    setEqualSplitDescription("");
                    setEqualSplitAmount("");
                    if (groupId) {
                      void loadEqualSplitMembers(groupId);
                      void loadExpenses(groupId);
                    } else {
                      setExpenses([]);
                    }
                  }}
                  required
                >
                  <option value="">Select a group</option>
                  {groups.map((group) => (
                    <option key={group.id} value={group.id}>
                      {group.name || `Group ${group.id}`}
                    </option>
                  ))}
                </select>

                {equalSplitMembersLoading ? (
                  <p role="status">Loading group members…</p>
                ) : equalSplitMembersError ? (
                  <p className="form-error" role="alert">
                    {equalSplitMembersError}
                  </p>
                ) : equalSplitGroupId ? (
                  <p className="form-hint">
                    {equalSplitMembers.length} actual group member
                    {equalSplitMembers.length === 1 ? "" : "s"} will be included.
                  </p>
                ) : null}

                <label htmlFor="equal-split-existing-expense">Existing Expense</label>
                <select
                  id="equal-split-existing-expense"
                  value={equalSplitExpenseId}
                  onChange={(event) => {
                    const expenseId = event.target.value ? Number(event.target.value) : "";
                    setEqualSplitExpenseId(expenseId);
                    const expense = expenses.find((item) => item.id === expenseId);
                    setEqualSplitDescription(expense?.description ?? "");
                    setEqualSplitAmount(expense ? String(expense.amount) : "");
                    if (expense) {
                      setEqualSplitPayerId(String(expense.paidBy.id));
                    }
                    setEqualSplitReview(false);
                  }}
                  required
                >
                  <option value="">Select an existing expense</option>
                  {expenses.map((expense) => (
                    <option key={expense.id} value={expense.id}>
                      {expense.description} — ₹{expense.amount.toFixed(2)}
                    </option>
                  ))}
                </select>
                <p className="form-hint">
                  Select an expense already saved in this group. Its saved description and amount will be used.
                </p>

                <label htmlFor="equal-split-payer">Paid by</label>
                <select
                  id="equal-split-payer"
                  value={equalSplitPayerId}
                  onChange={(event) => setEqualSplitPayerId(event.target.value)}
                  required
                >
                  <option value="">Select payer</option>
                  {equalSplitMembers.map((member) => (
                    <option key={member.user.id} value={member.user.id}>
                      {member.user.name}
                    </option>
                  ))}
                </select>

                <div className="form-actions">
                  <button
                    type="button"
                    className="primary-button"
                    onClick={reviewEqualSplit}
                    disabled={equalSplitMembersLoading || !equalSplitMembers.length}
                  >
                    Review Split
                  </button>
                  <button
                    type="button"
                    className="secondary-button"
                    onClick={() => setShowEqualSplitWizard(false)}
                  >
                    Cancel
                  </button>
                </div>
              </div>
            ) : (
              <div>
                <div className="selected-expense">
                  <div><span>Expense</span><strong>{equalSplitDescription}</strong></div>
                  <div>
                    <span>Total amount</span>
                    <strong>₹{Number(equalSplitAmount).toFixed(2)}</strong>
                  </div>
                </div>
                <p className="form-hint">
                  Adjust each participant’s amount below. The total of all amounts must equal the expense total.
                </p>
                <div className="split-preview">
                  {equalSplitMembers.map((member) => {
                    const sharePaise = equalSplitSharesPaise[member.user.id] ?? 0;
                    return (
                      <div className="split-member" key={member.user.id}>
                        <div className="mini-avatar">
                          {member.user.name.charAt(0).toUpperCase()}
                        </div>
                        <strong>{member.user.name}</strong>
                        <label className="split-amount-editor">
                          <span className="sr-only">Amount for {member.user.name}</span>
                          ₹
                          <input
                            type="number"
                            min="0"
                            step="0.01"
                            inputMode="decimal"
                            value={(sharePaise / 100).toFixed(2)}
                            onChange={(event) => {
                              const value = event.target.value;
                              if (value === "") {
                                setEqualSplitSharesPaise((current) => ({ ...current, [member.user.id]: 0 }));
                                return;
                              }
                              const parsed = Number(value);
                              if (Number.isFinite(parsed) && parsed >= 0
                                  && Math.abs(parsed * 100 - Math.round(parsed * 100)) < 0.000001) {
                                setEqualSplitSharesPaise((current) => ({ ...current, [member.user.id]: Math.round(parsed * 100) }));
                              }
                            }}
                          />
                        </label>
                      </div>
                    );
                  })}
                </div>
                <p className="form-hint">
                  Allocated: ₹{(equalSplitMembers.reduce((sum, member) => sum + (equalSplitSharesPaise[member.user.id] ?? 0), 0) / 100).toFixed(2)}
                  {equalSplitMembers.reduce((sum, member) => sum + (equalSplitSharesPaise[member.user.id] ?? 0), 0) === equalSplitTotalPaise
                    ? " — total matches."
                    : ` — difference ₹${((equalSplitTotalPaise - equalSplitMembers.reduce((sum, member) => sum + (equalSplitSharesPaise[member.user.id] ?? 0), 0)) / 100).toFixed(2)}.`}
                </p>
                <div className="form-actions">
                  <button
                    type="button"
                    className="primary-button"
                    onClick={() => void submitEqualSplitExpense()}
                    disabled={equalSplitSubmitting}
                  >
                    {equalSplitSubmitting ? "Splitting…" : "Confirm Split"}
                  </button>
                  <button
                    type="button"
                    className="secondary-button"
                    onClick={() => setEqualSplitReview(false)}
                    disabled={equalSplitSubmitting}
                  >
                    Edit
                  </button>
                </div>
              </div>
            )}
          </div>
        )}

        {/* =========================
            ADD EXPENSE
        ========================= */}

        {showAddExpense && (

          <div className="dashboard-card form-card">

            <div className="card-heading">

              <div>

                <p className="card-label">
                  NEW TRANSACTION
                </p>

                <h2>
                  Add Expense
                </h2>

              </div>

            </div>

            <form onSubmit={createExpense}>

              <label>
                Description
              </label>

              <input
                type="text"
                placeholder="e.g. Dinner, Groceries, Rent..."
                value={expenseDescription}
                onChange={(e) =>
                  setExpenseDescription(
                    e.target.value
                  )
                }
                required
              />

              <label>
                Amount
              </label>

              <input
                type="number"
                placeholder="Enter amount"
                value={expenseAmount}
                onChange={(e) =>
                  setExpenseAmount(
                    e.target.value
                  )
                }
                min="1"
                step="0.01"
                required
              />

              <label>
                Paid By
              </label>

              <select
                value={paidByUserId}
                onChange={(e) =>
                  setPaidByUserId(
                    e.target.value
                  )
                }
                required
              >

                <option value="">
                  Select who paid
                </option>

                {members.map(
                  (member) => (

                    <option
                      key={member.user.id}
                      value={member.user.id}
                    >
                      {member.user.name}
                    </option>

                  )
                )}

              </select>

              <div className="form-actions">

                <button
                  type="submit"
                  className="primary-button"
                >
                  Create Expense
                </button>

                <button
                  type="button"
                  className="secondary-button"
                  onClick={() => {
                    setShowAddExpense(false);
                    setExpenseDescription("");
                    setExpenseAmount("");
                  }}
                >
                  Cancel
                </button>

              </div>

            </form>

          </div>
        )}

        {/* =========================
            EXPENSES
        ========================= */}

        {showExpenses && (

          <div className="dashboard-card">

            <div className="card-heading">

              <div>

                <p className="card-label">
                  TRANSACTIONS
                </p>

                <h2>
                  Group Expenses
                </h2>

              </div>

              <span className="count-badge">
                {expenses.length}
              </span>

            </div>

            {expenses.length === 0 ? (

              <div className="empty-state">

                <div>
                  ₹
                </div>

                <p>
                  No expenses found.
                </p>

              </div>

            ) : (

              <div className="expense-list">

                {expenses.map(
                  (expense) => (

                    <div
                      className="expense-box modern-expense"
                      key={expense.id}
                      role="button"
                      tabIndex={0}
                      onClick={() => openSplitExpense(expense)}
                      onKeyDown={(event) => {
                        if (event.key === "Enter" || event.key === " ") {
                          event.preventDefault();
                          openSplitExpense(expense);
                        }
                      }}
                    >

                      <div className="expense-icon">
                        ₹
                      </div>

                      <div className="expense-main">

                        <h3>
                          {expense.description}
                        </h3>

                        <p>

                          Paid by{" "}

                          <strong>
                            {expense.paidBy.name}
                          </strong>

                        </p>

                        {expense.createdAt && (

                          <small>

                            {new Date(
                              expense.createdAt
                            ).toLocaleDateString()}{" "}

                            ·{" "}

                            {new Date(
                              expense.createdAt
                            ).toLocaleTimeString()}

                          </small>

                        )}

                      </div>

                      <div className="expense-side">

                        <strong>
                          ₹
                          {expense.amount.toFixed(
                            2
                          )}
                        </strong>



                      </div>

                    </div>
                  )
                )}

              </div>
            )}

          </div>
        )}

        {/* =========================
            SPLIT EXPENSE
        ========================= */}

        {showSplitForm &&
          selectedExpense && (

            <div className="dashboard-card">

              <div className="card-heading">

                <div>

                  <p className="card-label">
                    EXPENSE SPLITTING
                  </p>

                  <h2>
                    Split Expense
                  </h2>

                </div>

              </div>

              <div className="selected-expense">

                <div>

                  <span>
                    Expense
                  </span>

                  <strong>
                    {selectedExpense.description}
                  </strong>

                </div>

                <div>

                  <span>
                    Total Amount
                  </span>

                  <strong>

                    ₹
                    {selectedExpense.amount.toFixed(
                      2
                    )}

                  </strong>

                </div>

              </div>

              {splits.length === 0 ? (

                <div>

                  <div className="split-info">

                    <h3>
                      Automatic Equal Split
                    </h3>

                    <p>
                      The expense will be divided
                      equally among all group
                      members.
                    </p>

                  </div>

                  <div className="split-preview">

                    {members.map(
                      (member, index) => {

                        const totalPaise =
                          Math.round(
                            selectedExpense.amount *
                              100
                          );

                        const memberCount =
                          members.length;

                        const baseShare =
                          Math.floor(
                            totalPaise /
                              memberCount
                          );

                        const remainder =
                          totalPaise %
                          memberCount;

                        const share =
                          baseShare +
                          (index < remainder
                            ? 1
                            : 0);

                        return (

                          <div
                            className="split-member"
                            key={member.id}
                          >

                            <div className="mini-avatar">

                              {member.user.name
                                .charAt(0)
                                .toUpperCase()}

                            </div>

                            <strong>
                              {member.user.name}
                            </strong>

                            <span>

                              ₹
                              {(
                                share / 100
                              ).toFixed(2)}

                            </span>

                          </div>
                        );
                      }
                    )}

                  </div>

                  <div className="form-actions">

                    <button
                      type="button"
                      className="primary-button"
                      onClick={
                        createEqualSplits
                      }
                    >
                      Split Equally
                    </button>

                    <button
                      type="button"
                      className="secondary-button"
                      onClick={() => {
                        setShowSplitForm(false);
                        setSelectedExpense(null);
                        setSplits([]);
                      }}
                    >
                      Cancel
                    </button>

                  </div>

                </div>

              ) : (

                <div>

                  <div className="split-info">

                    <h3>
                      Current Split Details
                    </h3>

                  </div>

                  <div className="split-preview">

                    {splits.map(
                      (split) => (

                        <div
                          className="split-member"
                          key={split.id}
                        >

                          <div className="mini-avatar">

                            {split.user.name
                              .charAt(0)
                              .toUpperCase()}

                          </div>

                          <strong>
                            {split.user.name}
                          </strong>

                          <span>

                            ₹
                            {split.amount.toFixed(
                              2
                            )}

                          </span>

                          <span
                            className={
                              split.paid
                                ? "status paid"
                                : "status pending"
                            }
                          >
                            {split.paid
                              ? "Paid"
                              : "Pending"}
                          </span>

                          {!split.paid
                            && split.user.email.toLowerCase() === email.toLowerCase() && (
                            <>
                              <button
                                type="button"
                                onClick={() => payNow(split.id)}
                              >
                                Pay Now
                              </button>
                              <button
                                type="button"
                                onClick={() => void recoverPayment(split.id)}
                              >
                                Check Payment Status
                              </button>
                            </>
                          )}

                        </div>
                      )
                    )}

                  </div>

                </div>
              )}

            </div>
          )}

      </main>

    </div>
  );
}

export default App;