import streamlit as st
import pandas as pd
import plotly.graph_objects as go


# ---------------------------------------------------------
# Configuration
# ---------------------------------------------------------

st.set_page_config(
    page_title="Cloud Confidence",
    page_icon="☁️",
    layout="wide",
    initial_sidebar_state="expanded"
)


# ---------------------------------------------------------
# Custom CSS
# ---------------------------------------------------------

st.markdown(
    """
    <style>

    /* ---------- Global ---------- */

    .stApp {
        background-color: #f5f7fa;
        color: #263238;
    }

    .main {
        background-color: #f5f7fa;
    }

    /* Remove excessive top padding */
    .block-container {
        padding-top: 1.5rem;
        padding-bottom: 2rem;
    }


    /* ---------- Sidebar ---------- */

    [data-testid="stSidebar"] {
        background-color: #ffffff;
        border-right: 1px solid #e3e8ee;
    }

    [data-testid="stSidebar"] h2,
    [data-testid="stSidebar"] h3 {
        color: #37474f;
    }

    [data-testid="stSidebar"] label {
        color: #546e7a;
        font-weight: 500;
    }


    /* ---------- Header ---------- */

    .cloud-header {
        position: relative;
        height: 230px;
        border-radius: 18px;
        overflow: hidden;
        margin-bottom: 1.5rem;

        background:
            linear-gradient(
                90deg,
                rgba(31, 57, 72, 0.78),
                rgba(80, 120, 145, 0.38)
            ),
            url("https://images.unsplash.com/photo-1534088568595-a066f410bcda?auto=format&fit=crop&w=1800&q=85");

        background-size: cover;
        background-position: center 55%;

        box-shadow:
            0 6px 20px rgba(38, 50, 56, 0.10);
    }

    .cloud-header-content {
        position: absolute;
        left: 42px;
        top: 50%;
        transform: translateY(-50%);
        color: white;
    }

    .cloud-header-content h1 {
        font-size: 2.6rem;
        font-weight: 700;
        margin: 0;
        letter-spacing: -0.5px;
    }

    .cloud-header-content p {
        font-size: 1.05rem;
        margin-top: 8px;
        color: #edf5f8;
    }


    /* ---------- Section titles ---------- */

    .section-title {
        font-size: 1.15rem;
        font-weight: 650;
        color: #37474f;
        margin-top: 0.5rem;
        margin-bottom: 0.8rem;
    }


    /* ---------- KPI cards ---------- */

    .metric-card {
        background: white;
        border: 1px solid #e4e9ee;
        border-radius: 14px;
        padding: 18px 20px;
        box-shadow: 0 3px 12px rgba(38, 50, 56, 0.05);
        height: 100%;
    }

    .metric-label {
        color: #78909c;
        font-size: 0.82rem;
        font-weight: 600;
        text-transform: uppercase;
        letter-spacing: 0.6px;
    }

    .metric-value {
        color: #37474f;
        font-size: 1.45rem;
        font-weight: 700;
        margin-top: 5px;
    }


    /* ---------- Chart container ---------- */

    .chart-card {
        background: #ffffff;
        border: 1px solid #e4e9ee;
        border-radius: 16px;
        padding: 10px 12px 5px 12px;
        box-shadow: 0 3px 14px rgba(38, 50, 56, 0.05);
    }


    /* ---------- Info box ---------- */

    .info-box {
        background: #eef6fa;
        border-left: 4px solid #64a5c4;
        border-radius: 8px;
        padding: 12px 16px;
        margin-top: 1rem;
        color: #455a64;
        font-size: 0.9rem;
    }


    /* ---------- Footer ---------- */

    .footer {
        text-align: center;
        color: #90a4ae;
        font-size: 0.78rem;
        margin-top: 2rem;
        padding-top: 1rem;
        border-top: 1px solid #e1e6ea;
    }

    </style>
    """,
    unsafe_allow_html=True
)


# ---------------------------------------------------------
# Header
# ---------------------------------------------------------

st.markdown(
    """
    <div class="cloud-header">
        <div class="cloud-header-content">
            <h1>☁️ Cloud Confidence</h1>
            <p>Forecast performance and observation analysis</p>
        </div>
    </div>
    """,
    unsafe_allow_html=True
)


# ---------------------------------------------------------
# Load data
# ---------------------------------------------------------

@st.cache_data
def load_data():
    df = pd.read_csv("forecast.csv", sep=";")

    # Convert types
    df["time"] = pd.to_numeric(df["time"], errors="coerce")
    df["avgForecastError"] = pd.to_numeric(
        df["avgForecastError"],
        errors="coerce"
    )
    df["avgObservationValue"] = pd.to_numeric(
        df["avgObservationValue"],
        errors="coerce"
    )

    return df


df = load_data()


# ---------------------------------------------------------
# Sidebar filters
# ---------------------------------------------------------

st.sidebar.markdown(
    """
    <div style="
        font-size: 1.35rem;
        font-weight: 700;
        color: #37474f;
        margin-bottom: 0.2rem;
    ">
        ☁️ Cloud Confidence
    </div>

    <div style="
        color: #90a4ae;
        font-size: 0.85rem;
        margin-bottom: 1.5rem;
    ">
        Analysis filters
    </div>
    """,
    unsafe_allow_html=True
)

st.sidebar.markdown("### Filters")

stations = sorted(df["stationName"].dropna().unique())

selected_station = st.sidebar.selectbox(
    "Station",
    stations
)

variables = sorted(
    df.loc[
        df["stationName"] == selected_station,
        "variable"
    ].dropna().unique()
)

selected_variable = st.sidebar.selectbox(
    "Variable",
    variables
)

st.sidebar.markdown("---")

st.sidebar.caption(
    "Select a station and variable to explore forecast performance."
)


# ---------------------------------------------------------
# Filter data
# ---------------------------------------------------------

filtered = df[
    (df["stationName"] == selected_station)
    & (df["variable"] == selected_variable)
    ].copy()

filtered = filtered.sort_values("time")


# ---------------------------------------------------------
# Summary information
# ---------------------------------------------------------

unit = filtered["unit"].iloc[0] if not filtered.empty else ""

number_of_points = len(filtered)

mean_error = (
    filtered["avgForecastError"].mean()
    if not filtered.empty
    else 0
)

mean_value = (
    filtered["avgObservationValue"].mean()
    if not filtered.empty
    else 0
)


# ---------------------------------------------------------
# Summary cards
# ---------------------------------------------------------

st.markdown(
    '<div class="section-title">Current selection</div>',
    unsafe_allow_html=True
)

col1, col2, col3, col4 = st.columns(4)

with col1:
    st.markdown(
        f"""
        <div class="metric-card">
            <div class="metric-label">Station</div>
            <div class="metric-value">{selected_station}</div>
        </div>
        """,
        unsafe_allow_html=True
    )

with col2:
    st.markdown(
        f"""
        <div class="metric-card">
            <div class="metric-label">Variable</div>
            <div class="metric-value">{selected_variable}</div>
        </div>
        """,
        unsafe_allow_html=True
    )

with col3:
    st.markdown(
        f"""
        <div class="metric-card">
            <div class="metric-label">Mean Error</div>
            <div class="metric-value">
                {mean_error:.2f} {unit}
            </div>
        </div>
        """,
        unsafe_allow_html=True
    )

with col4:
    st.markdown(
        f"""
        <div class="metric-card">
            <div class="metric-label">Data Points</div>
            <div class="metric-value">{number_of_points}</div>
        </div>
        """,
        unsafe_allow_html=True
    )


st.markdown("<br>", unsafe_allow_html=True)


# ---------------------------------------------------------
# Chart
# ---------------------------------------------------------

if filtered.empty:

    st.warning(
        "No data is available for this station and variable."
    )

else:

    fig = go.Figure()

    fig.add_trace(
        go.Scatter(
            x=filtered["time"],
            y=filtered["avgObservationValue"],
            mode="lines+markers",
            name="Observation",

            line=dict(
                color="#4F91B5",
                width=3
            ),

            marker=dict(
                color="#FFFFFF",
                size=8,
                line=dict(
                    color="#4F91B5",
                    width=2
                )
            ),

            error_y=dict(
                type="data",
                array=filtered["avgForecastError"],
                visible=True,
                color="#A9C7D6",
                thickness=1.5,
                width=5
            ),

            hovertemplate=(
                "<b>Time: %{x}</b><br>"
                "Observation: %{y:.2f}"
                f" {unit}<br>"
                "Forecast error: %{error_y.array:.2f}"
                f" {unit}"
                "<extra></extra>"
            )
        )
    )

    fig.update_layout(

        title=dict(
            text=(
                f"<b>{selected_variable}</b>"
                f"<br><span style='font-size:13px;"
                f"color:#78909C'>{selected_station}</span>"
            ),
            x=0.02,
            xanchor="left"
        ),

        xaxis=dict(
            title=dict(
                text="Forecast time",
                font=dict(
                    color="#607D8B"
                )
            ),
            showgrid=True,
            gridcolor="#E9EEF2",
            zeroline=False,
            tickfont=dict(
                color="#78909C"
            )
        ),

        yaxis=dict(
            title=dict(
                text=f"Value ({unit})",
                font=dict(
                    color="#607D8B"
                )
            ),
            showgrid=True,
            gridcolor="#E9EEF2",
            zeroline=False,
            tickfont=dict(
                color="#78909C"
            )
        ),

        hovermode="x unified",

        paper_bgcolor="#FFFFFF",
        plot_bgcolor="#FFFFFF",

        height=560,

        margin=dict(
            l=65,
            r=30,
            t=80,
            b=60
        ),

        legend=dict(
            orientation="h",
            yanchor="bottom",
            y=1.02,
            xanchor="right",
            x=1,
            font=dict(
                color="#546E7A"
            )
        )
    )

    # -----------------------------------------------------
    # Chart card
    # -----------------------------------------------------

    st.markdown(
        '<div class="section-title">Forecast performance</div>',
        unsafe_allow_html=True
    )

    st.markdown(
        '<div class="chart-card">',
        unsafe_allow_html=True
    )

    st.plotly_chart(
        fig,
        use_container_width=True,
        config={
            "displayModeBar": True,
            "displaylogo": False,
            "modeBarButtonsToRemove": [
                "lasso2d",
                "select2d"
            ]
        }
    )

    st.markdown("</div>", unsafe_allow_html=True)


# ---------------------------------------------------------
# Information
# ---------------------------------------------------------

st.markdown(
    """
    <div class="info-box">
        <b>How to read the chart</b><br>
        The blue line represents the observed value.
        The light blue error bars represent the average forecast error
        for each forecast time.
    </div>
    """,
    unsafe_allow_html=True
)


# ---------------------------------------------------------
# Footer
# ---------------------------------------------------------

st.markdown(
    """
    <div class="footer">
        Cloud Confidence · Forecast verification dashboard
    </div>
    """,
    unsafe_allow_html=True
)
