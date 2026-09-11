import { BrowserRouter, NavLink, Navigate, Route, Routes } from 'react-router-dom'
import { useEffect, useMemo, useState } from 'react'
import { isSupabaseConfigured, supabase } from './lib/supabase'
import './App.css'

const formatMoney = (value) =>
  new Intl.NumberFormat('uz-UZ', {
    style: 'currency',
    currency: 'UZS',
    maximumFractionDigits: 0,
  }).format(Number(value || 0))

const createEmptyForm = (direction = 'received') => ({
  name: '',
  amount: '',
  returned: '',
  direction,
  isPaid: false,
})

const formatCalculatorResult = (value) => {
  if (!Number.isFinite(value)) {
    return 'Noto‘g‘ri ifoda'
  }

  const roundedValue = Number(value.toFixed(10))
  return roundedValue.toString()
}

const evaluateExpression = (expression) => {
  const normalizedExpression = expression.replace(/×/g, '*').replace(/÷/g, '/').replace(/,/g, '.').trim()

  if (!normalizedExpression) {
    return 0
  }

  if (!/^[0-9+\-*/().\s]+$/.test(normalizedExpression)) {
    throw new Error('Invalid expression')
  }

  const result = Function(`"use strict"; return (${normalizedExpression});`)()

  if (!Number.isFinite(result)) {
    throw new Error('Invalid expression')
  }

  return result
}

const getUserLabel = (user) => user?.user_metadata?.full_name || user?.email || 'Qarz daftari foydalanuvchisi'

const getDebtLoadErrorMessage = (error) => {
  const message = error?.message || ''

  if (message.includes('does not exist') || message.includes('relation')) {
    return 'Ma’lumotlarni olishda muammo yuz berdi. Supabase “debts” jadvali yaratilmagan bo‘lishi mumkin. schema.sql ni qayta ishlating.'
  }

  if (message.toLowerCase().includes('permission') || message.toLowerCase().includes('row-level security')) {
    return 'Ma’lumotlarni olishda muammo yuz berdi. Supabase RLS siyosatini va auth sozlamalarini tekshiring. Shuningdek, authenticated role uchun public.debts jadvaliga ruxsat berilganligiga ishonch hosil qiling.'
  }

  return 'Ma’lumotlarni yuklashda muammo yuz berdi.'
}

function App() {
  const [session, setSession] = useState(null)
  const [debts, setDebts] = useState([])
  const [loading, setLoading] = useState(true)
  const [pageError, setPageError] = useState('')

  useEffect(() => {
    if (!isSupabaseConfigured || !supabase) {
      setLoading(false)
      return
    }

    let isMounted = true

    const syncSession = async () => {
      const {
        data: { session: currentSession },
        error,
      } = await supabase.auth.getSession()

      if (!isMounted) return

      if (error) {
        setPageError('Ma’lumotlarni yuklashda muammo yuz berdi.')
      }

      setSession(currentSession)
      setLoading(false)
    }

    syncSession()

    const {
      data: { subscription },
    } = supabase.auth.onAuthStateChange((_event, currentSession) => {
      if (!isMounted) return
      setSession(currentSession)
      setLoading(false)
    })

    return () => {
      isMounted = false
      subscription.unsubscribe()
    }
  }, [])

  useEffect(() => {
    if (!session || !supabase) {
      setDebts([])
      return
    }

    let isMounted = true

    const loadDebts = async () => {
      const { data, error } = await supabase
        .from('debts')
        .select('*')
        .eq('user_id', session.user.id)
        .order('created_at', { ascending: false })

      if (!isMounted) return

      if (error) {
        console.error('Supabase debts load error:', error)
        setPageError(getDebtLoadErrorMessage(error))
        return
      }

      setDebts(data ?? [])
      setPageError('')
    }

    loadDebts()

    return () => {
      isMounted = false
    }
  }, [session])

  const handleLogout = async () => {
    if (!supabase) return

    const { error } = await supabase.auth.signOut()
    if (error) {
      setPageError('Chiqishda muammo yuz berdi.')
      return
    }

    setSession(null)
  }

  if (!isSupabaseConfigured) {
    return <ConfigurationScreen />
  }

  if (loading) {
    return (
      <div className="auth-page">
        <div className="auth-panel loading-panel">
          <p className="eyebrow">Qarz daftari</p>
          <h1>Yuklanmoqda...</h1>
        </div>
      </div>
    )
  }

  return (
    <BrowserRouter>
      {session ? (
        <AppLayout
          user={session.user}
          debts={debts}
          setDebts={setDebts}
          onLogout={handleLogout}
          pageError={pageError}
        />
      ) : (
        <AuthScreen />
      )}
    </BrowserRouter>
  )
}

function ConfigurationScreen() {
  return (
    <div className="auth-page">
      <div className="auth-panel config-panel">
        <div className="auth-brand">
          <img className="brand-logo" src="/logo.svg" alt="Qarz daftari logotipi" />
          <div>
            <p className="eyebrow">Qarz daftari</p>
            <h1>Supabase konfiguratsiyasi kerak</h1>
          </div>
        </div>

        <p className="config-text">
          Loyiha ishlashi uchun .env faylida quyidagi ko‘rsatkichlarni kiriting:
        </p>

        <pre className="env-box">VITE_SUPABASE_URL=https://your-project.supabase.co
VITE_SUPABASE_ANON_KEY=your-anon-key</pre>

        <p className="config-text">
          Keyin Supabase dasturiga o‘tib, schema SQL faylini ishlatib tables yarating.
        </p>
      </div>
    </div>
  )
}

function AuthScreen() {
  const [mode, setMode] = useState('login')
  const [form, setForm] = useState({
    name: '',
    email: '',
    password: '',
  })
  const [error, setError] = useState('')
  const [info, setInfo] = useState('')

  const handleModeChange = (nextMode) => {
    setMode(nextMode)
    setError('')
    setInfo('')
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')
    setInfo('')

    if (!supabase) {
      setError('Supabase konfiguratsiyasi topilmadi. .env faylini to‘ldiring.')
      return
    }

    const email = form.email.trim()
    const password = form.password.trim()

    if (!email || !password) {
      setError('Email va parolni kiriting.')
      return
    }

    if (mode === 'register' && !form.name.trim()) {
      setError('Ismni kiriting.')
      return
    }

    try {
      if (mode === 'login') {
        const { error: signInError } = await supabase.auth.signInWithPassword({
          email,
          password,
        })

        if (signInError) {
          setError('Kirishda muammo yuz berdi. Email yoki parolni tekshirib ko‘ring.')
          return
        }

        return
      }

      const { data, error: signUpError } = await supabase.auth.signUp({
        email,
        password,
        options: {
          data: {
            full_name: form.name.trim(),
          },
        },
      })

      if (signUpError) {
        setError('Ro‘yxatdan o‘tishda muammo yuz berdi. Iltimos, qayta urinib ko‘ring.')
        return
      }

      if (data.session) {
        return
      }

      setInfo('Ro‘yhatdan o‘tish uchun emailingizga yuborilgan tasdiqlash linkini bosing.')
      setForm({ name: '', email: '', password: '' })
    } catch (submitError) {
      setError('Kirish/ro‘yxatdan o‘tishda muammo yuz berdi.')
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-panel">
        <div className="auth-brand">
          <img className="brand-logo" src="/logo.svg" alt="Qarz daftari logotipi" />
          <div>
            <p className="eyebrow">Qarz daftari</p>
            <h1>Qarz daftari</h1>
          </div>
        </div>

        <div className="auth-toggle">
          <button
            type="button"
            className={mode === 'login' ? 'active' : ''}
            onClick={() => handleModeChange('login')}
          >
            Kirish
          </button>
          <button
            type="button"
            className={mode === 'register' ? 'active' : ''}
            onClick={() => handleModeChange('register')}
          >
            Ro‘yxatdan o‘tish
          </button>
        </div>

        <form className="auth-form" onSubmit={handleSubmit}>
          {mode === 'register' && (
            <label>
              Ism
              <input
                type="text"
                value={form.name}
                onChange={(event) =>
                  setForm((current) => ({ ...current, name: event.target.value }))
                }
                placeholder="Ismingiz"
              />
            </label>
          )}

          <label>
            Email
            <input
              type="email"
              value={form.email}
              onChange={(event) =>
                setForm((current) => ({ ...current, email: event.target.value }))
              }
              placeholder="you@example.com"
            />
          </label>

          <label>
            Parol
            <input
              type="password"
              value={form.password}
              onChange={(event) =>
                setForm((current) => ({ ...current, password: event.target.value }))
              }
              placeholder="••••••••"
            />
          </label>

          {error && <p className="error-text">{error}</p>}
          {info && <p className="success-text">{info}</p>}

          <button type="submit" className="primary-btn">
            {mode === 'login' ? 'Kirish' : 'Ro‘yxatdan o‘tish'}
          </button>
        </form>
      </div>
    </div>
  )
}

function AppLayout({ user, debts, setDebts, onLogout, pageError }) {
  const navItems = [
    { to: '/', label: 'Asosiy sahifa' },
    { to: '/kalkulyator', label: 'Ichki kalkulyator' },
    { to: '/qarzdorman', label: 'Qarzdorman' },
    { to: '/qarzdorlar', label: 'Qarzdorlar' },
  ]

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand-block">
          <img className="brand-logo sidebar-logo" src="/logo.svg" alt="Qarz daftari logotipi" />
          <div>
            <p className="eyebrow">Qarz daftari</p>
            <h2>Boshqaruv paneli</h2>
          </div>
        </div>

        <nav className="nav-menu">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) => (isActive ? 'nav-item active' : 'nav-item')}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="user-card">
          <p className="card-label">Tizim foydalanuvchisi</p>
          <h3>{getUserLabel(user)}</h3>
          <span>{user.email}</span>
          <button type="button" className="secondary-btn" onClick={onLogout}>
            Chiqish
          </button>
        </div>
      </aside>

      <main className="main-content">
        <header className="topbar">
          <div>
            <p className="eyebrow">Umumiy ko‘rinish</p>
            <h1>Qarzlar boshqaruvi</h1>
          </div>
        </header>

        {pageError && <div className="global-error">{pageError}</div>}

        <Routes>
          <Route
            path="/"
            element={<DashboardPage debts={debts} setDebts={setDebts} user={user} />}
          />
          <Route path="/kalkulyator" element={<CalculatorPage />} />
          <Route
            path="/qarzdorman"
            element={
              <DebtPage
                pageType="qarzdorman"
                debts={debts.filter((item) => item.direction === 'given')}
              />
            }
          />
          <Route
            path="/qarzdorlar"
            element={
              <DebtPage
                pageType="qarzdorlar"
                debts={debts.filter((item) => item.direction === 'received')}
              />
            }
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  )
}

function CalculatorPage() {
  const [expression, setExpression] = useState('')
  const [error, setError] = useState('')
  const [history, setHistory] = useState(() => {
    try {
      const savedHistory = localStorage.getItem('qarzDaftariCalculatorHistory')
      return savedHistory ? JSON.parse(savedHistory) : []
    } catch {
      return []
    }
  })

  useEffect(() => {
    localStorage.setItem('qarzDaftariCalculatorHistory', JSON.stringify(history))
  }, [history])

  const previewResult = useMemo(() => {
    if (!expression.trim()) {
      return '0'
    }

    try {
      return formatCalculatorResult(evaluateExpression(expression))
    } catch {
      return 'Noto‘g‘ri ifoda'
    }
  }, [expression])

  const appendValue = (value) => {
    if (value === 'C') {
      setExpression('')
      setError('')
      return
    }

    if (value === 'DEL') {
      setExpression((current) => current.slice(0, -1))
      setError('')
      return
    }

    if (value === '=') {
      if (!expression.trim()) {
        return
      }

      try {
        const result = evaluateExpression(expression)
        const formattedResult = formatCalculatorResult(result)

        setExpression(formattedResult)
        setHistory((current) => [
          {
            id: `${Date.now()}-${Math.random()}`,
            expression: expression.trim(),
            result: formattedResult,
          },
          ...current,
        ].slice(0, 8))
        setError('')
      } catch {
        setError('Noto‘g‘ri ifoda')
      }
      return
    }

    if (value === '±') {
      setExpression((current) => {
        if (!current.trim()) {
          return current
        }

        return current.startsWith('-') ? current.slice(1) : `-${current}`
      })
      setError('')
      return
    }

    setExpression((current) => `${current}${value}`)
    setError('')
  }

  const buttons = [
    'C',
    'DEL',
    '±',
    '÷',
    '7',
    '8',
    '9',
    '×',
    '4',
    '5',
    '6',
    '-',
    '1',
    '2',
    '3',
    '+',
    '0',
    '.',
    '(',
    ')',
    '=',
  ]

  return (
    <div className="page-stack">
      <section className="calculator-card">
        <div className="section-heading">
          <h2>Ichki kalkulyator</h2>
        </div>

        <div className="calculator-display">
          <div className="calculator-expression">{expression || '0'}</div>
          <div className="calculator-result">{error || previewResult}</div>
        </div>

        <div className="calculator-grid">
          {buttons.map((button) => (
            <button
              key={button}
              type="button"
              className={`calculator-btn ${button === '=' ? 'equals' : ''}`}
              onClick={() => appendValue(button)}
            >
              {button}
            </button>
          ))}
        </div>

        {history.length > 0 && (
          <div className="calculator-history">
            <div className="history-header">
              <h3>Tarix</h3>
              <button
                type="button"
                className="history-clear"
                onClick={() => setHistory([])}
              >
                Tozalash
              </button>
            </div>

            <ul className="history-list">
              {history.map((item) => (
                <li key={item.id} className="history-item">
                  <span>{item.expression}</span>
                  <strong>{item.result}</strong>
                </li>
              ))}
            </ul>
          </div>
        )}
      </section>
    </div>
  )
}

function DashboardPage({ debts, setDebts, user }) {
  const [form, setForm] = useState(() => createEmptyForm('received'))
  const [submitError, setSubmitError] = useState('')

  const summary = useMemo(() => {
    const incoming = debts.filter((item) => item.direction === 'received')
    const outgoing = debts.filter((item) => item.direction === 'given')

    const totalIncoming = incoming.reduce((sum, item) => sum + Number(item.amount || 0), 0)
    const totalOutgoing = outgoing.reduce((sum, item) => sum + Number(item.amount || 0), 0)
    const totalReceived = incoming.reduce((sum, item) => sum + Number(item.returned || 0), 0)
    const totalPaid = outgoing.reduce((sum, item) => sum + Number(item.returned || 0), 0)

    return {
      incoming,
      outgoing,
      totalIncoming,
      totalOutgoing,
      totalReceived,
      totalPaid,
    }
  }, [debts])

  const handleSubmit = async (event) => {
    event.preventDefault()
    setSubmitError('')

    if (!supabase) {
      setSubmitError('Supabase konfiguratsiyasi topilmadi.')
      return
    }

    const isValid = form.name.trim() && form.amount && Number(form.amount) > 0

    if (!isValid) {
      setSubmitError('Ism va miqdorni to‘g‘ri kiriting.')
      return
    }

    const payload = {
      user_id: user.id,
      name: form.name.trim(),
      amount: Number(form.amount),
      returned: form.isPaid ? Number(form.amount) : Number(form.returned || 0),
      direction: form.direction,
    }

    const { data, error } = await supabase.from('debts').insert([payload]).select()

    if (error) {
      setSubmitError('Qarz saqlashda muammo yuz berdi.')
      return
    }

    setDebts((current) => [data?.[0], ...current])
    setForm(createEmptyForm(form.direction))
  }

  return (
    <div className="page-stack">
      <section className="summary-grid">
        <div className="summary-card accent">
          <p>Olgan qarzlar</p>
          <h3>{summary.incoming.length}</h3>
          <span>{formatMoney(summary.totalIncoming)}</span>
        </div>
        <div className="summary-card blue">
          <p>Bergan qarzlar</p>
          <h3>{summary.outgoing.length}</h3>
          <span>{formatMoney(summary.totalOutgoing)}</span>
        </div>
        <div className="summary-card green">
          <p>Qaytarganlar</p>
          <h3>{formatMoney(summary.totalReceived)}</h3>
          <span>Olgan qarzlar bo‘yicha</span>
        </div>
        <div className="summary-card orange">
          <p>To‘langanlar</p>
          <h3>{formatMoney(summary.totalPaid)}</h3>
          <span>Bergan qarzlar bo‘yicha</span>
        </div>
      </section>

      <section className="form-card">
        <div className="section-heading">
          <h2>Yangi qarz qo‘shish</h2>
        </div>

        <form className="debt-form" onSubmit={handleSubmit}>
          <label>
            Ism
            <input
              type="text"
              value={form.name}
              onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
              placeholder="Masalan: Oybek"
            />
          </label>

          <label>
            Miqdor
            <input
              type="number"
              value={form.amount}
              min="0"
              onChange={(event) =>
                setForm((current) => ({ ...current, amount: event.target.value }))
              }
              placeholder="0"
            />
          </label>

          <label className="checkbox-label">
            <span>Qarz to‘langan</span>
            <input
              type="checkbox"
              checked={form.isPaid}
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  isPaid: event.target.checked,
                }))
              }
            />
          </label>

          <label>
            Turi
            <select
              value={form.direction}
              onChange={(event) =>
                setForm((current) => ({ ...current, direction: event.target.value }))
              }
            >
              <option value="received">Olgan qarzlar</option>
              <option value="given">Bergan qarzlar</option>
            </select>
          </label>

          <button type="submit" className="primary-btn">
            Saqlash
          </button>
        </form>

        {submitError && <p className="error-text mt-12">{submitError}</p>}
      </section>

      <section className="table-grid">
        <div className="table-card">
          <div className="table-header">
            <h3>Olgan qarzlar</h3>
            <span>{summary.incoming.length} ta</span>
          </div>
          <table>
            <thead>
              <tr>
                <th>Ism</th>
                <th>Miqdor</th>
                <th>Qaytarishim kerak</th>
                <th>Qaytardim</th>
              </tr>
            </thead>
            <tbody>
              {summary.incoming.length ? (
                summary.incoming.map((item) => (
                  <tr key={item.id}>
                    <td>{item.name}</td>
                    <td>{formatMoney(item.amount)}</td>
                    <td>{formatMoney(Math.max(Number(item.amount || 0) - Number(item.returned || 0), 0))}</td>
                    <td>{formatMoney(item.returned)}</td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="4" className="empty-cell">
                    Hozircha ma’lumot yo‘q
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <div className="table-card">
          <div className="table-header">
            <h3>Bergan qarzlar</h3>
            <span>{summary.outgoing.length} ta</span>
          </div>
          <table>
            <thead>
              <tr>
                <th>Ism</th>
                <th>Miqdor</th>
                <th>Qaytarishim kerak</th>
                <th>Qaytardim</th>
              </tr>
            </thead>
            <tbody>
              {summary.outgoing.length ? (
                summary.outgoing.map((item) => (
                  <tr key={item.id}>
                    <td>{item.name}</td>
                    <td>{formatMoney(item.amount)}</td>
                    <td>{formatMoney(Math.max(Number(item.amount || 0) - Number(item.returned || 0), 0))}</td>
                    <td>{formatMoney(item.returned)}</td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="4" className="empty-cell">
                    Hozircha ma’lumot yo‘q
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  )
}

function DebtPage({ pageType, debts }) {
  const [viewMode, setViewMode] = useState('debts')

  const pageMeta = useMemo(
    () => ({
      qarzdorman: {
        heading: 'Qarzdorman',
        description: 'Men bergan qarzlar ro‘yxati',
        columns: ['Ism', 'Miqdor', 'Qaytarishim kerak', 'Qaytardim'],
      },
      qarzdorlar: {
        heading: 'Qarzdorlar',
        description: 'Menga qarzdorlar ro‘yxati',
        columns: ['Ism', 'Miqdor', 'Qaytarishim kerak', 'Qaytardi'],
      },
    }),
    [],
  )

  const rows = useMemo(() => {
    if (viewMode === 'people') {
      const grouped = new Map()

      debts.forEach((item) => {
        const current = grouped.get(item.name) || {
          id: item.name,
          name: item.name,
          amount: 0,
          returned: 0,
        }

        current.amount += Number(item.amount || 0)
        current.returned += Number(item.returned || 0)
        grouped.set(item.name, current)
      })

      return [...grouped.values()].map((item) => ({
        ...item,
        remaining: Math.max(Number(item.amount || 0) - Number(item.returned || 0), 0),
      }))
    }

    return debts.map((item) => ({
      ...item,
      remaining: Math.max(Number(item.amount || 0) - Number(item.returned || 0), 0),
    }))
  }, [debts, viewMode])

  return (
    <div className="page-stack">
      <section className="table-card page-header-card">
        <div>
          <p className="eyebrow">Ro‘yxat</p>
          <h2>{pageMeta[pageType].heading}</h2>
        </div>
        <p>{pageMeta[pageType].description}</p>
      </section>

      <section className="filter-card">
        <div className="filter-group">
          <span>Saralash:</span>
          <button
            type="button"
            className={viewMode === 'people' ? 'filter-btn active' : 'filter-btn'}
            onClick={() => setViewMode('people')}
          >
            Odamlar
          </button>
          <button
            type="button"
            className={viewMode === 'debts' ? 'filter-btn active' : 'filter-btn'}
            onClick={() => setViewMode('debts')}
          >
            Qarzlar
          </button>
        </div>
      </section>

      <section className="table-card">
        <table>
          <thead>
            <tr>
              {pageMeta[pageType].columns.map((column) => (
                <th key={column}>{column}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rows.length ? (
              rows.map((item) => (
                <tr key={item.id ?? item.name}>
                  <td>{item.name}</td>
                  <td>{formatMoney(item.amount)}</td>
                  <td>{formatMoney(item.remaining)}</td>
                  <td>{formatMoney(item.returned)}</td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="4" className="empty-cell">
                  Ushbu sahifa uchun ma’lumotlar yo‘q
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </section>
    </div>
  )
}

export default App
