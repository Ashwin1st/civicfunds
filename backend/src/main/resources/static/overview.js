/* ============================================================
   CivicFunds — the overview screen.

   One rule governs this file: it renders counts of records and
   never a sum of money. The API it calls cannot return a dollar
   total, so this page could not draw one even by mistake.
   ============================================================ */

(function () {
  'use strict';

  var cards  = document.getElementById('cards');
  var panels = document.getElementById('panels');
  var scope  = document.getElementById('scope');

  function esc(s) {
    if (s === null || s === undefined) { return ''; }
    return String(s)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }

  function n(v) { return (v || 0).toLocaleString('en-US'); }

  /** Source system names are stored uppercase with underscores. */
  function pretty(label) {
    return String(label).replace(/_/g, ' ');
  }

  function card(k, v, note, cls) {
    return '<div class="card-s ' + (cls || '') + '">' +
             '<div class="k">' + esc(k) + '</div>' +
             '<div class="v">' + esc(v) + '</div>' +
             '<div class="n">' + note + '</div>' +
           '</div>';
  }

  /**
   * A bar chart where the bar length is a share of the largest row, not of
   * the whole. Nothing here implies the categories add up to anything.
   */
  function panel(title, sub, rows, levelled) {
    if (!rows || !rows.length) { return ''; }
    var max = rows.reduce(function (m, r) { return Math.max(m, r.count); }, 0) || 1;

    var bars = rows.map(function (r) {
      var pct = Math.max(1.5, (r.count / max) * 100);
      var cls = levelled ? ' l-' + String(r.label).toUpperCase() : '';
      return '<div class="bar' + cls + '">' +
               '<div class="lb" title="' + esc(pretty(r.label)) + '">' + esc(pretty(r.label)) + '</div>' +
               '<div class="tr"><div class="fl" style="width:' + pct.toFixed(1) + '%"></div></div>' +
               '<div class="ct">' + n(r.count) + '</div>' +
             '</div>';
    }).join('');

    return '<div class="panel"><h2>' + esc(title) + '</h2>' +
           '<p class="sub">' + sub + '</p>' +
           '<div class="bars">' + bars + '</div></div>';
  }

  function render(d) {
    var keyPct = d.records ? Math.round((d.withGovernmentKey / d.records) * 100) : 0;

    cards.innerHTML =
      card('Records indexed', n(d.records),
           'each one keeps its source and the date it was fetched') +
      card('Source systems', n(d.sourceSystems),
           'of six planned — state, city and school district are not loaded yet') +
      card('With a government key', n(d.withGovernmentKey),
           keyPct + '% carry an identifier that can be joined without guessing') +
      card('Total public investment', 'Not shown',
           d.measurementClassCount + ' measurement classes are present. They answer ' +
           'different questions, so no sum of them would be true.', 'warn');

    panels.innerHTML =
      panel('By level of government',
            'Record counts. These do add up to the whole, which is why counting them ' +
            'this way is honest — <b>the dollar figures behind them do not and could ' +
            'not be charted</b>.',
            d.byGovernmentLevel, true) +

      panel('By measurement class',
            'What each figure actually measures. An obligation is money legally ' +
            'committed; an outlay is cash that left the treasury; a programmed amount ' +
            'is a plan revised on a cycle. <b>These are the classes the app refuses to ' +
            'add together.</b>',
            d.byMeasurementClass, false) +

      panel('By source system',
            'Names ending in MANUAL were transcribed from a published PDF by a person, ' +
            'not fetched by an adapter. <b>The app shows the difference rather than ' +
            'hiding it.</b>',
            d.bySourceSystem, false) +

      panel('By category',
            'Assigned from the award type and assistance listing. A record with no ' +
            'usable category is counted as uncategorised rather than guessed at.',
            d.byCategory, false) +

      panel('By place',
            'Place of performance as the source published it. Records with no city ' +
            'are counted and labelled — <b>a gap in the source is a fact about the ' +
            'source, not a reason to drop the record</b>.',
            d.byCity, false);

    if (scope) {
      scope.textContent = 'Duval · Clay · Nassau — ' + n(d.records) + ' records indexed';
    }
  }

  fetch('/api/meta/overview', { headers: { 'Accept': 'application/json' } })
    .then(function (r) {
      if (!r.ok) { throw new Error('HTTP ' + r.status); }
      return r.json();
    })
    .then(render)
    .catch(function (err) {
      cards.innerHTML =
        '<div class="state"><b>Could not reach the CivicFunds API.</b>' +
        '<p>This page is served by the same Spring Boot application as the API, so ' +
        'this usually means the backend is still starting. (' + esc(err.message) + ')</p></div>';
    });
})();
