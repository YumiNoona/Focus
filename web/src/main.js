import './style.css'

const downloadUrl = 'https://github.com/YumiNoona/Focus/releases/latest'

document.querySelector('#app').innerHTML = `
  <div class="noise" aria-hidden="true"></div>
  <header class="nav-wrap">
    <nav class="nav shell" aria-label="Main navigation">
      <a class="brand" href="#top" aria-label="FocuLY home">
        <img src="/assets/logo.png" alt="" />
        <span>FocuLY</span>
      </a>
      <button class="menu-toggle" type="button" aria-label="Open menu" aria-expanded="false" aria-controls="nav-links">
        <span></span><span></span>
      </button>
      <div class="nav-links" id="nav-links">
        <a href="#why">Why FocuLY</a>
        <a href="#inside">Inside the app</a>
        <a href="#privacy">Privacy</a>
        <a class="nav-cta" href="${downloadUrl}">Get the app <span>↗</span></a>
      </div>
    </nav>
  </header>

  <main id="top">
    <section class="hero shell">
      <div class="hero-copy reveal">
        <h1>A little less scroll.<br /><em>A little more you.</em></h1>
        <p class="hero-lede">FocuLY turns screen-time boundaries into a gentle daily rhythm—without accounts, ads, or another feed competing for your attention.</p>
        <div class="hero-actions">
          <a class="button primary" href="${downloadUrl}">Download for Android <span>↗</span></a>
          <a class="button text-button" href="#inside">See how it feels <span>↓</span></a>
        </div>
      </div>
      <div class="hero-visual reveal delay-1" aria-label="FocuLY app preview">
        <div class="orbit orbit-one"></div><div class="orbit orbit-two"></div>
        <div class="float-note note-one"><span>50:00</span><small>deep focus</small></div>
        <div class="float-note note-two"><span>3 apps</span><small>quietly paused</small></div>
        <div class="phone hero-phone">
          <div class="phone-speaker"></div>
          <img src="/assets/screen-home.png" alt="FocuLY home screen" />
        </div>
        <img class="hero-cat" src="/assets/cat-focus.png" alt="A cat calmly focusing at a desk" />
      </div>
    </section>

    <section class="principles shell reveal" id="why">
      <p class="section-kicker">Not productivity theatre</p>
      <div class="principle-heading">
        <h2>Boundaries that bend<br />without breaking.</h2>
      </div>
      <div class="mode-grid">
        <article class="mode-card mode-normal">
          <span class="mode-number">01</span><div class="mode-icon">○</div>
          <h3>Normal</h3><p>A calm popup and a deliberate way through when life needs flexibility.</p>
          <span class="mode-tag">Gentle pause</span>
        </article>
        <article class="mode-card mode-resistant">
          <span class="mode-number">02</span><div class="mode-icon">◐</div>
          <h3>Resistant</h3><p>A 30-second pause and a reason turn impulse into an intentional choice.</p>
          <span class="mode-tag">Mindful friction</span>
        </article>
        <article class="mode-card mode-strict">
          <span class="mode-number">03</span><div class="mode-icon">●</div>
          <h3>Strict</h3><p>A finite commitment with no early in-app exit, made for the moments that matter.</p>
          <span class="mode-tag">Clear boundary</span>
        </article>
      </div>
    </section>

    <section class="showcase" id="inside">
      <div class="shell showcase-grid">
        <div class="showcase-copy reveal">
          <p class="section-kicker">One calm place</p>
          <h2>Everything you need.<br />Nothing chasing you.</h2>
          <p>Move from intention to action without digging through settings. Every screen is designed to feel quiet, clear, and close at hand.</p>
          <div class="preview-tabs" role="tablist" aria-label="App screens">
            <button class="preview-tab active" type="button" role="tab" id="preview-tab-home" aria-controls="preview-panel" aria-selected="true" tabindex="0" data-screen="home"><b>Home</b><span>Start a session in one tap</span></button>
            <button class="preview-tab" type="button" role="tab" id="preview-tab-session" aria-controls="preview-panel" aria-selected="false" tabindex="-1" data-screen="session"><b>Focus</b><span>See only what matters now</span></button>
            <button class="preview-tab" type="button" role="tab" id="preview-tab-blocking" aria-controls="preview-panel" aria-selected="false" tabindex="-1" data-screen="blocking"><b>Pause</b><span>Meet distraction with intention</span></button>
            <button class="preview-tab" type="button" role="tab" id="preview-tab-insights" aria-controls="preview-panel" aria-selected="false" tabindex="-1" data-screen="insights"><b>Insights</b><span>Notice patterns, not scores</span></button>
          </div>
        </div>
        <div class="device-stage reveal delay-1" role="tabpanel" id="preview-panel" aria-labelledby="preview-tab-home" tabindex="0">
          <div class="stage-blob"></div>
          <div class="phone preview-phone">
            <div class="phone-speaker"></div>
            <img id="preview-image" src="/assets/screen-home.png" alt="FocuLY home screen" />
          </div>
          <div class="stage-caption"><span id="preview-index">01</span><span id="preview-name">Your calm starting point</span></div>
        </div>
      </div>
    </section>

    <section class="features shell">
      <div class="feature feature-wide reveal">
        <div>
          <p class="section-kicker">Built around real life</p>
          <h2>Today, every day,<br />or just for now.</h2>
        </div>
        <div class="rhythm-visual" aria-hidden="true">
          <div class="rhythm-label"><span>Deep work</span><b>50 min</b></div>
          <div class="timeline"><i></i><i></i><i></i><i></i><i></i><i></i><i></i></div>
          <div class="rhythm-days"><span>M</span><span>T</span><span>W</span><span>T</span><span>F</span><span class="off">S</span><span class="off">S</span></div>
        </div>
      </div>
      <article class="feature privacy-card reveal" id="privacy">
        <img src="/assets/cat-guard.png" alt="Cat guarding a small plant" />
        <p class="section-kicker">Private by default</p>
        <h3>Your attention is not data.</h3>
        <p>No account. No ads. No analytics. Your settings and focus history stay on your device.</p>
      </article>
      <article class="feature focus-card reveal delay-1">
        <div class="focus-orb"><img src="/assets/cat-pause.png" alt="Resting cat" /></div>
        <p class="section-kicker">Made for imperfect days</p>
        <h3>Focus, without the guilt.</h3>
        <p>Your goal is a guide, not a score. FocuLY makes space for breaks, changes of plan, and starting again.</p>
      </article>
    </section>

    <section class="final-cta shell reveal">
      <div class="cta-copy">
        <p class="section-kicker">Your next moment is yours</p>
        <h2>Make a little room<br />for what matters.</h2>
        <a class="button primary dark" href="${downloadUrl}">Get FocuLY for Android <span>↗</span></a>
        <p class="fine-print">Direct APK download from GitHub · Android 9 or newer</p>
      </div>
      <img src="/assets/cat-complete.png" alt="Happy cat celebrating a completed focus session" />
    </section>
  </main>

  <footer class="footer shell">
    <a class="brand" href="#top"><img src="/assets/logo.png" alt="" /><span>FocuLY</span></a>
    <p>A calmer relationship with your screen.</p>
  </footer>
`

const menu = document.querySelector('.menu-toggle')
const links = document.querySelector('.nav-links')
const setMenu = (open) => {
  menu.setAttribute('aria-expanded', String(open))
  menu.setAttribute('aria-label', open ? 'Close menu' : 'Open menu')
  links.classList.toggle('open', open)
}
menu.addEventListener('click', () => setMenu(menu.getAttribute('aria-expanded') !== 'true'))
links.querySelectorAll('a').forEach((link) => link.addEventListener('click', () => setMenu(false)))
document.addEventListener('keydown', (event) => {
  if (event.key === 'Escape' && menu.getAttribute('aria-expanded') === 'true') {
    setMenu(false)
    menu.focus()
  }
})
document.addEventListener('click', (event) => {
  if (!event.target.closest('.nav')) setMenu(false)
})

const previews = {
  home: ['/assets/screen-home.png', 'FocuLY home screen', '01', 'Your calm starting point'],
  session: ['/assets/screen-session.png', 'Active FocuLY focus session', '02', 'One thing at a time'],
  blocking: ['/assets/screen-blocking.png', 'FocuLY intentional pause screen', '03', 'A thoughtful interruption'],
  insights: ['/assets/screen-insights.png', 'FocuLY insights screen', '04', 'Patterns without pressure'],
}

const tabs = [...document.querySelectorAll('.preview-tab')]
let previewTimer
const selectPreview = (tab) => {
  window.clearTimeout(previewTimer)
  tabs.forEach((item) => {
    item.classList.remove('active')
    item.setAttribute('aria-selected', 'false')
    item.tabIndex = -1
  })
  tab.classList.add('active')
  tab.setAttribute('aria-selected', 'true')
  tab.tabIndex = 0
  document.querySelector('#preview-panel').setAttribute('aria-labelledby', tab.id)
  const [src, alt, index, label] = previews[tab.dataset.screen]
  const image = document.querySelector('#preview-image')
  image.classList.add('switching')
  previewTimer = window.setTimeout(() => {
    image.src = src
    image.alt = alt
    document.querySelector('#preview-index').textContent = index
    document.querySelector('#preview-name').textContent = label
    image.classList.remove('switching')
  }, 180)
}
tabs.forEach((tab, index) => {
  tab.addEventListener('click', () => selectPreview(tab))
  tab.addEventListener('keydown', (event) => {
    const target = { ArrowRight: (index + 1) % tabs.length, ArrowLeft: (index + tabs.length - 1) % tabs.length, Home: 0, End: tabs.length - 1 }[event.key]
    if (target === undefined) return
    event.preventDefault()
    tabs[target].focus()
    selectPreview(tabs[target])
  })
})

if ('IntersectionObserver' in window) {
const observer = new IntersectionObserver((entries) => {
  entries.forEach((entry) => {
    if (entry.isIntersecting) {
      entry.target.classList.add('visible')
      observer.unobserve(entry.target)
    }
  })
}, { threshold: 0.14 })
document.querySelectorAll('.reveal').forEach((element) => observer.observe(element))
} else {
  document.querySelectorAll('.reveal').forEach((element) => element.classList.add('visible'))
}

const nav = document.querySelector('.nav-wrap')
window.addEventListener('scroll', () => nav.classList.toggle('scrolled', window.scrollY > 24), { passive: true })
