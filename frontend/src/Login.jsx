import { useState } from "react";
import {
  HeartPulse,
  Lock,
  User,
  Eye,
  EyeOff,
  ShieldCheck,
  AlertCircle,
  ChevronRight,
  Loader2,
} from "lucide-react";
import { loginApi } from "./api";

export default function Login({ onLoginSuccess }) {
  const [usernameOrEmail, setUsernameOrEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");

    const trimmedUser = usernameOrEmail.trim();
    if (!trimmedUser) {
      setError("Please enter your username or registered email address.");
      return;
    }

    if (!password) {
      setError("Please enter your password.");
      return;
    }

    setLoading(true);

    try {
      const data = await loginApi(trimmedUser, password);
      if (onLoginSuccess) {
        onLoginSuccess(data);
      }
    } catch (err) {
      setError(err.message || "Authentication failed. Please verify your credentials.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="login-container">
      <style>{`
        .login-container {
          min-height: 100vh;
          display: flex;
          align-items: center;
          justify-content: center;
          background: linear-gradient(135deg, #0b1f2b 0%, #102a3a 50%, #15394d 100%);
          padding: 24px;
          box-sizing: border-box;
          font-family: "DM Sans", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
          color: #172d40;
        }

        .login-card {
          width: 100%;
          max-width: 440px;
          background: #ffffff;
          border-radius: 20px;
          box-shadow: 0 25px 60px -15px rgba(0, 0, 0, 0.4), 0 0 0 1px rgba(255, 255, 255, 0.1);
          overflow: hidden;
        }

        .login-header {
          padding: 38px 34px 26px;
          text-align: center;
          background: #fbfcfd;
          border-bottom: 1px solid #edf2f6;
        }

        .login-logo {
          width: 56px;
          height: 56px;
          border-radius: 16px;
          background: linear-gradient(135deg, #159a83 0%, #0d6d5d 100%);
          color: white;
          display: inline-flex;
          align-items: center;
          justify-content: center;
          margin-bottom: 18px;
          box-shadow: 0 8px 18px rgba(21, 154, 131, 0.35);
        }

        .login-header h1 {
          margin: 0;
          font-family: "Manrope", sans-serif;
          font-size: 24px;
          font-weight: 800;
          color: #102b3a;
          letter-spacing: -0.5px;
        }

        .login-header p {
          margin: 8px 0 0;
          font-size: 13px;
          color: #647b8c;
        }

        .login-body {
          padding: 32px 34px 36px;
        }

        .error-box {
          display: flex;
          align-items: flex-start;
          gap: 10px;
          background: #fff3f2;
          border: 1px solid #fecdd3;
          border-radius: 10px;
          padding: 12px 14px;
          margin-bottom: 22px;
          color: #b91c1c;
          font-size: 13px;
          line-height: 1.4;
        }

        .form-group {
          margin-bottom: 20px;
        }

        .form-label {
          display: block;
          font-size: 12px;
          font-weight: 700;
          text-transform: uppercase;
          letter-spacing: 0.6px;
          color: #4b6272;
          margin-bottom: 8px;
        }

        .input-wrapper {
          position: relative;
          display: flex;
          align-items: center;
        }

        .input-icon {
          position: absolute;
          left: 14px;
          color: #8c9fae;
          pointer-events: none;
        }

        .login-input {
          width: 100%;
          padding: 12px 14px 12px 42px;
          border: 1.5px solid #dbe3ea;
          border-radius: 10px;
          font-size: 14px;
          color: #162a38;
          background: #fdfdfd;
          transition: all 0.2s ease;
          box-sizing: border-box;
        }

        .login-input:focus {
          outline: none;
          border-color: #159a83;
          background: #ffffff;
          box-shadow: 0 0 0 3px rgba(21, 154, 131, 0.15);
        }

        .password-toggle {
          position: absolute;
          right: 12px;
          background: none;
          border: none;
          color: #8c9fae;
          cursor: pointer;
          display: flex;
          align-items: center;
          padding: 4px;
          border-radius: 6px;
        }

        .password-toggle:hover {
          color: #3b5060;
        }

        .submit-btn {
          width: 100%;
          padding: 14px 20px;
          background: linear-gradient(135deg, #159a83 0%, #107765 100%);
          color: white;
          border: none;
          border-radius: 10px;
          font-size: 14px;
          font-weight: 700;
          cursor: pointer;
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 9px;
          transition: all 0.2s ease;
          margin-top: 26px;
          box-shadow: 0 4px 14px rgba(21, 154, 131, 0.3);
        }

        .submit-btn:hover:not(:disabled) {
          background: linear-gradient(135deg, #18a990 0%, #12826e 100%);
          transform: translateY(-1px);
          box-shadow: 0 6px 18px rgba(21, 154, 131, 0.4);
        }

        .submit-btn:disabled {
          opacity: 0.7;
          cursor: not-allowed;
          transform: none;
        }

        .login-footer {
          padding: 18px 34px;
          background: #f8fafc;
          border-top: 1px solid #edf2f6;
          text-align: center;
          font-size: 11px;
          color: #7f91a0;
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 7px;
        }
      `}</style>

      <div className="login-card">
        <div className="login-header">
          <div className="login-logo">
            <HeartPulse size={32} />
          </div>
          <h1>CareGrid</h1>
          <p>Government Hospital Resource Allocation System</p>
        </div>

        <div className="login-body">
          {error && (
            <div className="error-box">
              <AlertCircle size={18} style={{ flexShrink: 0, marginTop: 1 }} />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label className="form-label" htmlFor="usernameOrEmail">
                Username or Email
              </label>
              <div className="input-wrapper">
                <User size={18} className="input-icon" />
                <input
                  id="usernameOrEmail"
                  type="text"
                  className="login-input"
                  placeholder="Enter username or email"
                  value={usernameOrEmail}
                  onChange={(e) => setUsernameOrEmail(e.target.value)}
                  autoComplete="username"
                  disabled={loading}
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="password">
                Password
              </label>
              <div className="input-wrapper">
                <Lock size={18} className="input-icon" />
                <input
                  id="password"
                  type={showPassword ? "text" : "password"}
                  className="login-input"
                  placeholder="Enter your password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  autoComplete="current-password"
                  disabled={loading}
                />
                <button
                  type="button"
                  className="password-toggle"
                  onClick={() => setShowPassword(!showPassword)}
                  aria-label={showPassword ? "Hide password" : "Show password"}
                  tabIndex={-1}
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>

            <button type="submit" className="submit-btn" disabled={loading}>
              {loading ? (
                <>
                  <Loader2 size={18} className="animate-spin" />
                  <span>Authenticating...</span>
                </>
              ) : (
                <>
                  <span>Sign In to CareGrid</span>
                  <ChevronRight size={17} />
                </>
              )}
            </button>
          </form>
        </div>

        <div className="login-footer">
          <ShieldCheck size={15} color="#159a83" />
          <span>CareGrid Hospital Resource Coordination · 256-bit BCrypt & JWT Protected</span>
        </div>
      </div>
    </div>
  );
}
