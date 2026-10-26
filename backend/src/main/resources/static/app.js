/* ============================================================
   CivicFunds — the whole front end.
   One api() helper and one render function per screen. No
   framework, no build step, no state library: the screens are
   driven by what the API returned and nothing else.

   The rule this file exists to keep: a figure is never rendered
   without its measurement class beside it, and nothing on this
   page ever adds two figures together.
   ============================================================ */

(function () {
  'use strict';

  var els = {
    form:     document.getElementById('form'),
    q:        document.getElementById('q'),
    results:  document.getElementById('results'),
    detail:   document.getElementById('detail'),
    coverage: document.getElementById('coverage'),
    scope:    document.getElementById('scope')
  };

  var lastHits = [];

  // ---------------------------------------------------------- helpers

  /** Every call to the backend goes through here. */
  function api(path) {
    return fetch(path, { headers: { 'Accept': 'application/json' } })
      .then(function (r) {
        if (!r.ok) { throw new Error('HTTP ' + r.status); }
        return r.json();
      });
  }

  /** Text goes through here before it reaches innerHTML. Source data is data. */
  function esc(s) {
    if (s === null || s === undefined) { return ''; }
    return String(s)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }

  /**
   * Format a figure. Returns null for null — an unpublished amount is not
   * zero, and this function will not invent one.
   */
  function money(raw) {
    if (raw === null || raw === undefined || raw === '') { return null; }
    var n = Number(raw);
    if (isNaN(n)) { return esc(raw); }
    // Sign goes outside the currency symbol. A de-obligation is a real,
    // published, negative figure and it should read like one.
    var digits = Math.abs(n).toLocaleString('en-US',
      { minimumFractionDigits: 0, maximumFractionDigits: 0 });
    return (n < 0 ? '-$' : '$') + digits;
  }

  function day(iso) {
    if (!iso) { return null; }
    var d = new Date(iso);
    if (isNaN(d.getTime())) { return esc(iso); }
    return d.toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' });
  }

  function show(el) { el.hidden = false; }
  function hide(el) { el.hidden = true; }

  function chip(cls, text) {
    return '<span class="chip ' + cls + '">' + esc(text) + '</span>';
  }

  // ---------------------------------------------------------- screen: results

  function renderResults(res) {
    lastHits = res.hits || [];
    hide(els.detail);

    if (!lastHits.length) {
      els.results.innerHTML =
        '<div class="state"><b>Nothing indexed matches &ldquo;' + esc(res.query) + '&rdquo;.</b>' +
        '<p>That is a statement about this index, not about reality. CivicFunds currently ' +
        'ingests federal awards for Duval, Clay and Nassau counties; state and city sources ' +
        'are not loaded yet, so their records cannot appear here.</p></div>';
      show(els.results);
      return;
    }

    var tallies = Object.keys(res.byMatchKind || {}).map(function (k) {
      return chip('m-' + k, k + ' ' + res.byMatchKind[k]);
    }).join(' ');

    var head =
      '<div class="rhead">' +
        '<h2>' + lastHits.length + ' record' + (lastHits.length === 1 ? '' : 's') +
        ' for &ldquo;' + esc(res.query) + '&rdquo;</h2>' +
        '<div class="tallies">' + tallies + '</div>' +
      '</div>';

    // The refusal comes BEFORE the records, not as a footnote after them.
    var notot = res.whyNoTotal
      ? '<div class="notot"><b>No combined total.</b> ' + esc(res.whyNoTotal) + '</div>'
      : '';

    var rows = lastHits.map(function (h, i) {
      var amount = money(h.amount);
      var amountHtml = amount
        ? '<div class="amt">' + amount + '<span class="cls">' + esc(h.amountClass) + '</span></div>'
        : '<div class="amt"><span class="none">No figure published in this record</span></div>';

      var meta = [];
      if (h.recipient)     { meta.push(esc(h.recipient)); }
      if (h.place)         { meta.push(esc(h.place)); }
      if (h.externalKey)   { meta.push('<span style="font-family:var(--font-mono)">' + esc(h.externalKey) + '</span>'); }
      meta.push(esc(h.sourceSystem));

      return '<button class="hit" data-i="' + i + '">' +
        '<div class="ht">' +
          '<div class="hn">' + esc(h.title || 'Untitled record') + '</div>' +
          '<div class="chips">' +
            chip('m-' + h.matchKind, h.matchKind) +
            chip('c-' + h.governmentLevel, h.governmentLevel) +
          '</div>' +
        '</div>' +
        amountHtml +
        '<div class="meta">' + meta.join(' <span>·</span> ') + '</div>' +
        '<div class="why"><b>How this was found:</b> ' + esc(h.matchNote) + '</div>' +
      '</button>';
    }).join('');

    els.results.innerHTML = head + notot + rows;
    show(els.results);

    Array.prototype.forEach.call(els.results.querySelectorAll('.hit'), function (b) {
      b.addEventListener('click', function () { renderDetail(lastHits[+b.dataset.i]); });
    });
  }

  // ---------------------------------------------------------- screen: detail

  /** What one measurement class actually means, in words a resident can use. */
  var CLASS_MEANING = {
    APPROPRIATED: 'Budget authority a legislature enacted. The money is allowed to be spent; it has not necessarily been committed or paid.',
    PROGRAMMED:   'Planned in a multi-year programme and revised on a cycle. A plan, not a commitment.',
    AWARDED:      'The value of an award or contract at the moment it was made.',
    OBLIGATED:    'The agency has legally committed this money. It is not a statement that the money has been paid out.',
    OUTLAYED:     'Cash that has actually left the treasury.',
    MATCH_REQUIRED: 'A matching contribution the recipient must provide. Not money the awarding government is paying.',
    ESTIMATED:    'An estimate published by the source, not a settled figure.',
    UNKNOWN:      'The source did not say what this figure measures, so CivicFunds will not guess.'
  };

  function row(label, value, cls) {
    if (value === null || value === undefined || value === '') {
      return '<dt>' + esc(label) + '</dt><dd class="empty">Not published in this record</dd>';
    }
    return '<dt>' + esc(label) + '</dt><dd class="' + (cls || '') + '">' + value + '</dd>';
  }

  function renderDetail(h) {
    hide(els.results);

    var amount = money(h.amount);
    var meaning = CLASS_MEANING[h.amountClass] || CLASS_MEANING.UNKNOWN;

    var figure = amount
      ? '<div class="big">' + amount + '</div><div class="bigcls">' + esc(h.amountClass) + '</div>'
      : '<div class="big" style="font-size:20px;color:var(--muted)">No figure published</div>';

    els.detail.innerHTML =
      '<button class="back" id="back">&larr; Back to results</button>' +
      '<div class="card">' +
        '<div class="chips" style="justify-content:flex-start;display:flex;gap:6px">' +
          chip('c-' + h.governmentLevel, h.governmentLevel) +
          chip('m-' + h.matchKind, h.matchKind) +
        '</div>' +
        '<h2>' + esc(h.title || 'Untitled record') + '</h2>' +
        figure +
        '<dl class="rows">' +
          row('Recipient', esc(h.recipient)) +
          row('Funding agency', esc(h.fundingAgency)) +
          row('Place', esc(h.place)) +
          row('Council district', h.councilDistrict ? esc(h.councilDistrict) : null) +
          row('Source key', esc(h.externalKey), 'mono') +
          row('Source system', esc(h.sourceSystem)) +
          row('Retrieved', day(h.retrievedAt), 'mono') +
          row('Source record',
              h.sourceUrl ? '<a href="' + esc(h.sourceUrl) + '" rel="noopener noreferrer" target="_blank">' +
                            esc(h.sourceUrl) + '</a>' : null) +
          row('How it was found', esc(h.matchNote)) +
        '</dl>' +
        '<div class="classnote">' +
          '<b>What &ldquo;' + esc(h.amountClass) + '&rdquo; means.</b> ' + esc(meaning) +
          '<br><br><b>Why there is no total on this page.</b> This is one record from one ' +
          'government. Other governments may have funded the same work and recorded it in ' +
          'their own units. CivicFunds will not add figures that measure different things, ' +
          'and it will not add figures that measure the same thing twice.' +
        '</div>' +
      '</div>';

    show(els.detail);
    document.getElementById('back').addEventListener('click', function () {
      hide(els.detail);
      show(els.results);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  // ---------------------------------------------------------- screen: coverage

  function renderCoverage(c) {
    if (!c) { return; }
    var stat = function (k, v, n) {
      return '<div class="stat"><div class="k">' + esc(k) + '</div>' +
             '<div class="v">' + esc(v) + '</div>' +
             '<div class="n">' + esc(n) + '</div></div>';
    };
    els.coverage.innerHTML = '<div class="card">' +
      stat('Records indexed', (c.normalizedRecords || 0).toLocaleString(), 'each with a source and a date') +
      stat('Source systems', c.sourceSystems ? c.sourceSystems.length : 0, 'of six planned') +
      stat('With a government key', (c.recordsWithDeterministicKey || 0).toLocaleString(), 'joinable without guessing') +
      stat('Total public investment', 'Not shown', 'measurement classes are not comparable') +
      '</div>';
    show(els.coverage);
    els.scope.textContent = 'Duval · Clay · Nassau — ' +
      (c.normalizedRecords || 0).toLocaleString() + ' records indexed';
  }

  // ---------------------------------------------------------- wiring

  function doSearch(term) {
    els.q.value = term;
    hide(els.detail);
    els.results.innerHTML = '<div class="state"><b><span class="spin"></span>Searching…</b></div>';
    show(els.results);

    api('/api/funding/search?q=' + encodeURIComponent(term) + '&limit=25')
      .then(renderResults)
      .catch(function (err) {
        els.results.innerHTML =
          '<div class="state"><b>Could not reach the CivicFunds API.</b>' +
          '<p>The front end is served by the same Spring Boot application as the API, so ' +
          'this usually means the backend is still starting. (' + esc(err.message) + ')</p></div>';
      });
  }

  els.form.addEventListener('submit', function (e) {
    e.preventDefault();
    var term = els.q.value.trim();
    if (term.length >= 2) { doSearch(term); }
  });

  Array.prototype.forEach.call(document.querySelectorAll('.ex'), function (b) {
    b.addEventListener('click', function () { doSearch(b.dataset.q); });
  });

  // Coverage is live from the API on load, so the header is never a guess.
  api('/api/meta/coverage').then(renderCoverage).catch(function () { /* stays hidden */ });
})();
