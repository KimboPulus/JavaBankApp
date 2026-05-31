import { useEffect, useMemo, useState } from "react";

const savedSession = () => {
  try {
    return JSON.parse(localStorage.getItem("bank-session")) || null;
  } catch {
    return null;
  }
};

async function api(path, options = {}, token) {
  const response = await fetch(path, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options.headers || {})
    }
  });

  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(error.message || "Something went wrong. Try again in a moment.");
  }

  if (response.status === 204) {
    return null;
  }
  return response.json();
}

function money(value, currency) {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency,
    minimumFractionDigits: 2
  }).format(Number(value || 0));
}

export default function App() {
  const [session, setSession] = useState(savedSession);
  const [mode, setMode] = useState("login");
  const [authForm, setAuthForm] = useState({
    username: "max",
    email: "",
    password: "password123"
  });
  const [accounts, setAccounts] = useState([]);
  const [newAccount, setNewAccount] = useState({
    accountType: "CHECKING",
    currency: "PLN"
  });
  const [transfer, setTransfer] = useState({
    senderAccountNumber: "",
    receiverAccountNumber: "",
    amount: ""
  });
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);

  const primaryAccount = useMemo(() => accounts[0], [accounts]);

  useEffect(() => {
    if (!session) {
      localStorage.removeItem("bank-session");
      return;
    }

    localStorage.setItem("bank-session", JSON.stringify(session));
    loadAccounts(session.token);
  }, [session]);

  useEffect(() => {
    const senderStillExists = accounts.some(
      (account) => account.accountNumber === transfer.senderAccountNumber
    );

    if (primaryAccount && (!transfer.senderAccountNumber || !senderStillExists)) {
      setTransfer((current) => ({
        ...current,
        senderAccountNumber: primaryAccount.accountNumber
      }));
    } else if (!primaryAccount && transfer.senderAccountNumber) {
      setTransfer((current) => ({
        ...current,
        senderAccountNumber: ""
      }));
    }
  }, [accounts, primaryAccount, transfer.senderAccountNumber]);

  async function loadAccounts(token = session?.token) {
    if (!token) {
      return;
    }

    const data = await api("/api/accounts", {}, token);
    setAccounts(data);
  }

  async function submitAuth(event) {
    event.preventDefault();
    setBusy(true);
    setMessage("");

    try {
      const path = mode === "login" ? "/api/auth/login" : "/api/auth/register";
      const payload =
        mode === "login"
          ? { username: authForm.username, password: authForm.password }
          : authForm;
      const data = await api(path, {
        method: "POST",
        body: JSON.stringify(payload)
      });
      setSession(data);
      setMessage(
        mode === "login"
          ? "You are signed in."
          : "Profile created. Your first bank account is ready."
      );
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusy(false);
    }
  }

  async function submitTransfer(event) {
    event.preventDefault();
    setBusy(true);
    setMessage("");

    try {
      await api(
        "/api/transfers",
        {
          method: "POST",
          body: JSON.stringify({
            ...transfer,
            amount: Number(transfer.amount)
          })
        },
        session.token
      );
      setTransfer((current) => ({ ...current, receiverAccountNumber: "", amount: "" }));
      await loadAccounts();
      setMessage("Transfer sent.");
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusy(false);
    }
  }

  async function submitNewAccount(event) {
    event.preventDefault();
    setBusy(true);
    setMessage("");

    try {
      const account = await api(
        "/api/accounts",
        {
          method: "POST",
          body: JSON.stringify(newAccount)
        },
        session.token
      );
      await loadAccounts();
      setMessage(`New ${account.accountType.toLowerCase()} account opened.`);
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusy(false);
    }
  }

  async function closeAccount(account) {
    if (Number(account.balance) !== 0) {
      setMessage("Move the money out before closing this account.");
      return;
    }

    const confirmed = window.confirm(`Close account ${account.accountNumber}?`);
    if (!confirmed) {
      return;
    }

    setBusy(true);
    setMessage("");

    try {
      await api(
        `/api/accounts/${encodeURIComponent(account.accountNumber)}`,
        {
          method: "DELETE"
        },
        session.token
      );
      await loadAccounts();
      setMessage("Account closed.");
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusy(false);
    }
  }

  function signOut() {
    setSession(null);
    setAccounts([]);
    setMessage("");
  }

  if (!session) {
    return (
      <main className="auth-shell">
        <section className="auth-panel">
          <div>
            <p className="eyebrow">BankApp</p>
            <h1>Simple banking, without the noise.</h1>
            <p className="muted">
              Sign in with the demo user, or make a fresh account and try a transfer.
            </p>
          </div>

          <div className="tabs" aria-label="Choose auth mode">
            <button className={mode === "login" ? "active" : ""} onClick={() => setMode("login")}>
              Login
            </button>
            <button className={mode === "register" ? "active" : ""} onClick={() => setMode("register")}>
              Register
            </button>
          </div>

          <form onSubmit={submitAuth} className="stack">
            <label>
              Username
              <input
                value={authForm.username}
                onChange={(event) => setAuthForm({ ...authForm, username: event.target.value })}
                autoComplete="username"
                required
              />
            </label>

            {mode === "register" && (
              <label>
                Email
                <input
                  value={authForm.email}
                  onChange={(event) => setAuthForm({ ...authForm, email: event.target.value })}
                  type="email"
                  autoComplete="email"
                  required
                />
              </label>
            )}

            <label>
              Password
              <input
                value={authForm.password}
                onChange={(event) => setAuthForm({ ...authForm, password: event.target.value })}
                type="password"
                autoComplete={mode === "login" ? "current-password" : "new-password"}
                required
              />
            </label>

            <button className="primary" disabled={busy}>
              {busy ? "Working..." : mode === "login" ? "Sign in" : "Create account"}
            </button>
          </form>

          {message && <p className="notice">{message}</p>}
        </section>
      </main>
    );
  }

  return (
    <main className="dashboard">
      <header className="topbar">
        <div>
          <p className="eyebrow">BankApp</p>
          <h1>Welcome back, {session.username}.</h1>
        </div>
        <button className="ghost" onClick={signOut}>
          Sign out
        </button>
      </header>

      <section className="grid">
        <div className="panel accounts">
          <div className="panel-head">
            <h2>Your accounts</h2>
            <button className="ghost" onClick={() => loadAccounts()} disabled={busy}>
              Refresh
            </button>
          </div>

          {accounts.length === 0 ? (
            <p className="muted">No accounts found yet.</p>
          ) : (
            <div className="account-list">
              {accounts.map((account) => (
                <article className="account-card" key={account.accountNumber}>
                  <div>
                    <p>{account.accountType.toLowerCase()}</p>
                    <strong>{money(account.balance, account.currency)}</strong>
                  </div>
                  <div className="account-actions">
                    <span>{account.accountNumber}</span>
                    <button
                      className="danger"
                      onClick={() => closeAccount(account)}
                      disabled={busy || Number(account.balance) !== 0}
                      title={
                        Number(account.balance) === 0
                          ? "Close this account"
                          : "Move the money out before closing"
                      }
                    >
                      Close
                    </button>
                  </div>
                </article>
              ))}
            </div>
          )}

          <form onSubmit={submitNewAccount} className="account-open-form">
            <h3>Open a new bank account</h3>
            <div className="inline-fields">
              <label>
                Type
                <select
                  value={newAccount.accountType}
                  onChange={(event) =>
                    setNewAccount({ ...newAccount, accountType: event.target.value })
                  }
                >
                  <option value="CHECKING">Checking</option>
                  <option value="SAVINGS">Savings</option>
                </select>
              </label>

              <label>
                Currency
                <select
                  value={newAccount.currency}
                  onChange={(event) =>
                    setNewAccount({ ...newAccount, currency: event.target.value })
                  }
                >
                  <option value="PLN">PLN</option>
                  <option value="EUR">EUR</option>
                  <option value="USD">USD</option>
                  <option value="GBP">GBP</option>
                </select>
              </label>
            </div>

            <button className="primary" disabled={busy}>
              {busy ? "Opening..." : "Open account"}
            </button>
          </form>
        </div>

        <div className="panel">
          <h2>New transfer</h2>
          <form onSubmit={submitTransfer} className="stack">
            <label>
              From
              <select
                value={transfer.senderAccountNumber}
                onChange={(event) =>
                  setTransfer({ ...transfer, senderAccountNumber: event.target.value })
                }
                required
              >
                {accounts.map((account) => (
                  <option key={account.accountNumber} value={account.accountNumber}>
                    {account.accountNumber} - {money(account.balance, account.currency)}
                  </option>
                ))}
              </select>
            </label>

            <label>
              To account number
              <input
                value={transfer.receiverAccountNumber}
                onChange={(event) =>
                  setTransfer({ ...transfer, receiverAccountNumber: event.target.value })
                }
                placeholder="PL30303030303030303030303030"
                required
              />
            </label>

            <label>
              Amount
              <input
                value={transfer.amount}
                onChange={(event) => setTransfer({ ...transfer, amount: event.target.value })}
                type="number"
                min="0.01"
                step="0.01"
                placeholder="150.00"
                required
              />
            </label>

            <button className="primary" disabled={busy || accounts.length === 0}>
              {busy ? "Sending..." : "Send transfer"}
            </button>
          </form>

          {message && <p className="notice">{message}</p>}
        </div>
      </section>
    </main>
  );
}
