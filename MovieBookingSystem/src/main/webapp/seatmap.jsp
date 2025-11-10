<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    // seatmap.jsp expects request attributes: showId (String) and rows (List<Map>)
    String showId = (String) request.getAttribute("showId");
    java.util.List rows = (java.util.List) request.getAttribute("rows");
    if (rows == null) rows = new java.util.ArrayList();
    // context path for JS usage
    String ctx = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Movie Booking System</title>

<link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap" rel="stylesheet">

<style>
:root{
  --seat-size:108px;
  --gap:18px;
  --accent: #3b82f6;
  --mint:#a7f3d0;
  --amber:#fbbf24;
  --rose:#f87171;
  --bg1:#081020;
  --bg2:#122030;
}
*{box-sizing:border-box}
body{
  margin:0;
  font-family:'Outfit',sans-serif;
  min-height:100vh;
  background:
    radial-gradient(900px 700px at 10% 20%, #f59e0b10, transparent),
    radial-gradient(800px 600px at 80% 80%, #7c3aed10, transparent),
    linear-gradient(135deg,var(--bg1),var(--bg2));
  color:#f8fafc;
  -webkit-font-smoothing:antialiased;
}

.container{
  max-width:1220px;
  margin:18px auto 48px;
  padding:0 28px;
}

/* include header already styled in include file; keep spacing consistent */
.header{
  text-align:center;
  margin-bottom:12px;
}
h1{ margin:0; font-size:32px; font-weight:700; }
.subtitle{ color:#93c5fd55; margin-top:6px; font-size:14px; }

/* Row container: label left, seats block right */
.row-block{
  display:flex;
  align-items:flex-start;
  gap:20px;
  padding:18px 12px;
  margin:20px 0;
  background: linear-gradient(180deg, rgba(255,255,255,0.02), rgba(255,255,255,0.01));
  border-radius:12px;
  border:1px solid rgba(255,255,255,0.03);
}

/* row label column */
.row-label{
  width:120px;
  min-width:120px;
  color:var(--mint);
  font-weight:700;
  align-self:center;
  padding-left:8px;
}

/* seats grid: 5 columns => 5 seats per line */
.row-seats{
  display:grid;
  grid-template-columns: repeat(5, var(--seat-size));
  grid-auto-rows: var(--seat-size);
  gap: var(--gap);
  justify-content:start;
  align-items:center;
  padding:6px;
}

/* seat card */
.seat{
  width:var(--seat-size);
  height:var(--seat-size);
  border-radius:14px;
  display:flex;
  flex-direction:column;
  justify-content:center;
  align-items:center;
  text-align:center;
  padding:8px;
  cursor:pointer;
  transition: transform .14s ease, box-shadow .14s ease, border-color .14s ease;
  border:1px solid rgba(255,255,255,0.06);
  background: linear-gradient(180deg, rgba(255,255,255,0.02), rgba(255,255,255,0.01));
  box-shadow: 0 8px 18px rgba(0,0,0,0.45);
  user-select:none;
}
.seat:hover{ transform: translateY(-6px); box-shadow:0 18px 34px rgba(0,0,0,0.5); }

/* textual */
.seat .id{ font-weight:800; font-size:17px; color:#05254a; margin-bottom:6px; }
.seat .stat{ font-size:12px; color:#123; text-transform:capitalize; margin-bottom:6px; }
.seat .price{ font-size:12px; color:#0b3b10; font-weight:700; }

/* statuses */
.seat.AVAILABLE{
  background: linear-gradient(180deg, #f0fff4, #dcffea);
  border-color: #94f3c9;
  color:#064e3b;
}
.seat.LOCKED{
  background: linear-gradient(180deg,#fff7ed,#fff2d6);
  border-color: #ffd86a;
  color:#5a3b00;
  cursor:not-allowed;
}
.seat.BOOKED{
  background: linear-gradient(180deg,#fff1f2,#ffdede);
  border-color: #ff9b9b;
  color:#5a1515;
  cursor:not-allowed;
}

/* selection */
.seat.selected{
  outline: 5px solid rgba(59,130,246,0.15);
  box-shadow: 0 12px 36px rgba(59,130,246,0.18);
  transform: translateY(-10px) scale(1.03);
}

/* controls */
.controls{ display:flex; gap:12px; align-items:center; margin-top:22px; padding-left:140px; }
.btn{
  padding:10px 16px; border-radius:10px; border:0; font-weight:700; cursor:pointer;
}
.btn.primary{ background:linear-gradient(90deg,#2563eb,#60a5fa); color:white; box-shadow:0 8px 20px rgba(37,99,235,0.22); }
.btn.positive{ background:linear-gradient(90deg,#059669,#10b981); color:white; box-shadow:0 8px 20px rgba(5,150,105,0.18); }
.btn.ghost{ background:transparent; color:#cbd5e1; border:1px solid rgba(255,255,255,0.04); }

/* legend */
.legend{ display:flex; gap:18px; padding-left:140px; margin-top:20px; color:#cbd5e1; }
.legend .sw{ width:14px; height:14px; border-radius:3px; display:inline-block; margin-right:8px; }

/* modal */
#checkoutModal{ display:none; position:fixed; inset:0; background:rgba(0,0,0,0.6); backdrop-filter: blur(6px); align-items:center; justify-content:center; z-index:999; }
.modalBox{ width:520px; max-width:94%; background: linear-gradient(180deg, rgba(255,255,255,0.02), rgba(255,255,255,0.01)); border-radius:12px; padding:20px; border:1px solid rgba(255,255,255,0.04); box-shadow:0 30px 60px rgba(2,6,23,0.8); color:#e6eef8; }
.modalBox h3{ margin:6px 0 12px 0; text-align:left; color:#fff; }
.modalBox input{ width:100%; padding:10px 12px; border-radius:8px; margin:6px 0; border:1px solid rgba(255,255,255,0.06); background:rgba(255,255,255,0.02); color:#fff; }

.modal-actions{ display:flex; gap:8px; justify-content:flex-end; margin-top:12px; }
.errorBox{ color:#ff8b8b; font-weight:700; text-align:center; margin-top:8px; }

@media (max-width:1020px){
  .row-block{ flex-direction:column; align-items:center; gap:12px; padding:12px; }
  .row-label{ width:auto; min-width:auto; order:-1; }
  .controls, .legend{ padding-left:0; justify-content:center; }
  .row-seats{ grid-template-columns: repeat(5, minmax(64px, 1fr)); gap:12px; }
  :root{ --seat-size:88px; }
}
</style>
</head>
<body>

  <!-- include header (shows login/register or welcome + admin + logout) -->
  <jsp:include page="/WEB-INF/inlude/header.jsp" />

  <div class="container">
    <div class="header">
      <h1>Movie Seats</h1>
      <div class="subtitle">Choose seats — two neat lines of 5 seats per row for a symmetrical layout</div>
    </div>

    <!-- rows -->
    <% for (Object rObj : rows) {
         java.util.Map r = (java.util.Map) rObj;
         String rowId = (String) r.get("rowId");
         java.util.List seatList = (java.util.List) r.get("seats");
    %>
      <div class="row-block" data-row="<%=rowId%>">
        <div class="row-label">Row <%= rowId %></div>
        <div class="row-seats">
          <% for (Object sObj : seatList) {
               java.util.Map seat = (java.util.Map) sObj;
               String id = (String)seat.get("id");
               String status = (String)seat.get("status");
               Object priceObj = seat.get("price");
               String price = priceObj==null ? "0" : String.valueOf(priceObj);
               if (status == null) status = "AVAILABLE";
          %>
            <div class="seat <%=status%>" data-seat="<%=id%>" data-price="<%=price%>">
              <div class="id"><%=id%></div>
              <div class="stat"><%=status.toLowerCase()%></div>
              <div class="price">₹<%=price%></div>
            </div>
          <% } %>
        </div>
      </div>
    <% } %>

    <!-- controls -->
    <div class="controls">
      <button id="reserveBtn" class="btn primary">Reserve Selected Seats</button>
      <button id="confirmBtn" class="btn positive" style="display:none;">Confirm Booking (Pay)</button>
      <button id="refreshBtn" class="btn ghost">Refresh</button>
    </div>

    <div class="legend">
      <div><span class="sw" style="background:linear-gradient(#f0fff4,#dcffea); border:1px solid #94f3c9;"></span> AVAILABLE</div>
      <div><span class="sw" style="background:linear-gradient(#fff7ed,#fff2d6); border:1px solid #ffd86a;"></span> LOCKED</div>
      <div><span class="sw" style="background:linear-gradient(#fff1f2,#ffdede); border:1px solid #ff9b9b;"></span> BOOKED</div>
    </div>

  </div>

  <!-- checkout modal -->
  <div id="checkoutModal">
    <div class="modalBox">
      <h3>Payment Details</h3>
      <input id="cardName" placeholder="Card Holder">
      <input id="cardNumber" placeholder="Card Number">
      <div style="display:flex; gap:10px;">
        <input id="cardExpiry" placeholder="MM/YY">
        <input id="cardCvv" placeholder="CVV" type="password" style="width:120px;">
      </div>
      <div class="modal-actions">
        <button id="checkoutCancel" class="btn ghost">Cancel</button>
        <button id="checkoutPay" class="btn positive">Pay & Confirm</button>
      </div>
      <div id="checkoutError" class="errorBox"></div>
    </div>
  </div>

<script>
(() => {
  const $ = s => document.querySelector(s);
  const $$ = s => Array.from(document.querySelectorAll(s));
  const selected = new Set();
  let currentLockToken = null;

  function seatEl(id){ return document.querySelector(`.seat[data-seat="${id}"]`); }

  // delegate click: toggles AVAILABLE seats
  document.addEventListener('click', e => {
    const seat = e.target.closest('.seat');
    if (!seat) return;
    if (seat.classList.contains('LOCKED') || seat.classList.contains('BOOKED')) return;
    const id = seat.dataset.seat;
    if (selected.has(id)) { selected.delete(id); seat.classList.remove('selected'); }
    else { selected.add(id); seat.classList.add('selected'); }
  });

  function postForm(url, data){
    const params = new URLSearchParams();
    Object.keys(data).forEach(k=>{
      const v = data[k];
      if (Array.isArray(v)) v.forEach(x=>params.append(k,x));
      else params.append(k,v);
    });
    return fetch(url, { method:'POST', headers:{ 'Content-Type':'application/x-www-form-urlencoded' }, body: params.toString() });
  }

  async function reserveSelected(){
    if (selected.size === 0) return alert('Select seats first');
    const seats = Array.from(selected);
    const ctx = '<%=ctx%>';
    try {
      const resp = await postForm(ctx + '/lockSeats', { showId:'<%= showId == null ? "" : showId %>', seat: seats });
      if (resp.ok){
        const json = await resp.json();
        (json.lockedSeats || seats).forEach(id => {
          const el = seatEl(id);
          if (el) { el.classList.remove('AVAILABLE','selected'); el.classList.add('LOCKED'); }
        });
        currentLockToken = json.lockToken || null;
        // small non-blocking notification instead of full page alert
        console.info('Seats locked. Token:', currentLockToken);
        // show confirm button
        $('#confirmBtn').style.display='inline-block';
        selected.clear();
      } else {
        // show response text (server may return message), but avoid reloading unless necessary
        const text = await resp.text();
        alert('Lock failed: ' + (text || resp.status));
      }
    } catch(err){ alert('Network error: ' + err.message); }
  }

  $('#reserveBtn').addEventListener('click', reserveSelected);

  // modal open
  $('#confirmBtn').addEventListener('click', () => {
    if (!currentLockToken) return alert('No lock token — reserve seats first.');
    $('#checkoutError').innerText = '';
    $('#cardName').value=''; $('#cardNumber').value=''; $('#cardExpiry').value=''; $('#cardCvv').value='';
    $('#checkoutModal').style.display = 'flex';
  });
  $('#checkoutCancel').addEventListener('click', ()=> $('#checkoutModal').style.display='none');

  // confirm payment
  $('#checkoutPay').addEventListener('click', async () => {
    const token = currentLockToken;
    if (!token) { $('#checkoutError').innerText='Lock token missing.'; return; }
    const data = {
      showId:'<%= showId == null ? "" : showId %>',
      lockToken: token,
      cardNumber: $('#cardNumber').value,
      cardName: $('#cardName').value,
      cardExpiry: $('#cardExpiry').value,
      cardCvv: $('#cardCvv').value
    };
    const ctx = '<%=ctx%>';
    try {
      const resp = await postForm(ctx + '/confirmBooking', data);
      if (resp.ok){
        const json = await resp.json();
        $('#checkoutModal').style.display='none';
        alert('Payment successful. Booking ID: ' + (json.bookingId||'(none)'));
        // mark seats booked
        (json.seats || json.lockedSeats || []).forEach(id=>{
          const el = seatEl(id);
          if (el) { el.classList.remove('LOCKED'); el.classList.add('BOOKED'); }
        });
        currentLockToken = null;
        $('#confirmBtn').style.display='none';
      } else {
        const text = await resp.text();
        $('#checkoutError').innerText = text || ('Payment failed: ' + resp.status);
      }
    } catch(err){ $('#checkoutError').innerText = 'Server error: ' + err.message; }
  });

  $('#refreshBtn').addEventListener('click', ()=> location.reload());
})();
</script>
</body>
</html>
