package com.org.pdfplatform.exception;

import java.util.Locale;

/**
 * Renders the full-screen, HD, animated error page (self-contained HTML: no images, no external files, so it also works
 * when the rest of the app is broken). A colour theme and an animated scene come from the error's category.
 */
public final class ErrorPageRenderer {

    /** Everything the page shows. operation = what the person was doing, message = the specific reason. */
    public record PageModel(int status, String code, ErrorGuide.Guide guide, String message, String guidance,
                            String operation, String method, String path, String refId, String time) { }

    private ErrorPageRenderer() { }

    public static String esc(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String render(PageModel m) {
        ErrorGuide.Guide g = m.guide();
        StringBuilder steps = new StringBuilder();
        int n = 0;
        for (String s : g.steps()) {
            n++;
            steps.append("<li style=\"--i:").append(n).append("\"><b>").append(n).append("</b><span>").append(esc(s)).append("</span></li>");
        }
        StringBuilder bubbles = new StringBuilder();
        for (int i = 1; i <= 14; i++) {
            bubbles.append("<i style=\"--n:").append(i).append("\"></i>");
        }
        String specific = m.message() == null || m.message().isBlank() || m.message().equals(g.headline()) ? "" :
                "<div class=\"specific\"><span>The exact message</span>" + esc(m.message()) + "</div>";
        String extra = m.guidance() == null || m.guidance().isBlank() ? "" :
                "<div class=\"specific hint\"><span>For this case</span>" + esc(m.guidance()) + "</div>";
        String tip = g.tip() == null || g.tip().isBlank() ? "" :
                "<aside class=\"idea\"><div class=\"bulb\">\uD83D\uDCA1</div><div><h3>Out-of-the-box idea</h3><p>" + esc(g.tip()) + "</p></div></aside>";
        String statusText = m.status() > 0 ? String.valueOf(m.status()) : "!";
        String details = "Ref " + m.refId() + " | " + m.code() + " | " + m.operation() + " | " + m.method() + " " + m.path() + " | " + m.time();
        return "<!DOCTYPE html>\n<html lang=\"en\"><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n"
                + "<title>" + esc(g.headline()) + " - PDF Voice And Games</title>\n<style>" + css(g.color1(), g.color2()) + "</style></head>\n<body class=\"cat-"
                + esc(g.category().toLowerCase(Locale.ROOT)) + "\">\n<div class=\"bubbles\" aria-hidden=\"true\">" + bubbles + "</div>\n"
                + "<main class=\"card\" role=\"alert\">\n<header><div class=\"scene\">" + scene(g.category()) + "</div>\n<div class=\"head\"><div class=\"big\">"
                + esc(statusText) + "</div><h1><span class=\"emo\">" + g.emoji() + "</span> " + esc(g.headline()) + "</h1><p class=\"why\">" + esc(g.reason()) + "</p></div></header>\n"
                + specific + extra
                + "<section class=\"ctx\"><div><span>What you were doing</span><b>" + esc(m.operation()) + "</b></div><div><span>Address</span><b>" + esc(m.method() + " " + m.path())
                + "</b></div><div><span>Error code</span><b>" + esc(m.code()) + "</b></div></section>\n"
                + "<section><h2>How to fix it</h2><ol class=\"steps\">" + steps + "</ol></section>\n" + tip
                + "\n<footer><div class=\"btns\"><button type=\"button\" onclick=\"history.length>1?history.back():location.href='/'\">\u2B05 Go back</button>"
                + "<button type=\"button\" onclick=\"location.reload()\">\u21BB Try again</button><a class=\"btn\" href=\"/\">\uD83C\uDFE0 Home</a>"
                + "<button type=\"button\" id=\"cp\" data-t=\"" + esc(details) + "\">\uD83D\uDCCB Copy details</button></div>"
                + "<div class=\"ref\">Reference <b>" + esc(m.refId()) + "</b> &middot; " + esc(m.time()) + "</div></footer>\n</main>\n"
                + "<script>document.getElementById('cp').onclick=function(){var b=this,t=b.dataset.t;(navigator.clipboard?navigator.clipboard.writeText(t):Promise.reject()).then(function(){b.textContent='\\u2705 Copied'},function(){b.textContent='Select and copy: '+t})};</script>\n"
                + "</body></html>\n";
    }

    private static String css(String c1, String c2) {
        return """
            :root{--c1:%s;--c2:%s}
            *{box-sizing:border-box}
            html,body{margin:0;min-height:100%%}
            body{font-family:"Segoe UI",system-ui,Roboto,Arial,sans-serif;color:#fff;background:linear-gradient(125deg,var(--c1),var(--c2),#14082b,var(--c1));background-size:400%% 400%%;animation:flow 16s ease infinite;display:flex;align-items:center;justify-content:center;padding:24px;overflow-x:hidden}
            @keyframes flow{0%%{background-position:0%% 50%%}50%%{background-position:100%% 50%%}100%%{background-position:0%% 50%%}}
            .bubbles{position:fixed;inset:0;pointer-events:none;overflow:hidden}
            .bubbles i{position:absolute;bottom:-90px;left:calc(var(--n)*7%%);width:calc(14px + var(--n)*5px);height:calc(14px + var(--n)*5px);border-radius:50%%;background:rgba(255,255,255,.14);box-shadow:inset 0 0 12px rgba(255,255,255,.35);animation:rise calc(9s + var(--n)*1.1s) linear infinite;animation-delay:calc(var(--n)*-1.3s)}
            @keyframes rise{to{transform:translateY(-120vh) translateX(40px);opacity:.1}}
            .card{position:relative;z-index:1;width:min(980px,100%%);background:rgba(12,8,30,.58);backdrop-filter:blur(14px);-webkit-backdrop-filter:blur(14px);border:1px solid rgba(255,255,255,.28);border-radius:28px;padding:30px 34px 22px;box-shadow:0 24px 80px rgba(8,14,40,.5),0 0 0 6px rgba(255,255,255,.05);animation:pop .7s cubic-bezier(.2,1.4,.4,1) both}
            @keyframes pop{from{transform:translateY(40px) scale(.92);opacity:0}}
            header{display:flex;gap:26px;align-items:center;flex-wrap:wrap}
            .scene{flex:0 0 220px;width:220px;height:176px;filter:drop-shadow(0 10px 22px rgba(8,14,40,.45))}
            .scene svg{width:100%%;height:100%%;overflow:visible}
            .head{flex:1 1 320px;min-width:0}
            .big{font-size:clamp(58px,11vw,112px);font-weight:900;line-height:.9;letter-spacing:-3px;background:linear-gradient(90deg,#fff,var(--c2),#fff);background-size:200%% 100%%;-webkit-background-clip:text;background-clip:text;color:transparent;animation:shine 3.2s linear infinite}
            @keyframes shine{to{background-position:200%% 0}}
            h1{margin:8px 0 6px;font-size:clamp(22px,3.4vw,34px)}.emo{display:inline-block;animation:bounce 1.6s ease-in-out infinite}
            @keyframes bounce{50%%{transform:translateY(-8px) rotate(-6deg)}}
            .why{margin:0;font-size:1.06rem;line-height:1.5;color:#eaf1ff}
            .specific{margin:16px 0 0;padding:12px 16px;border-radius:14px;background:rgba(255,255,255,.1);border-left:6px solid var(--c2);font-family:Consolas,monospace;font-size:.95rem;word-break:break-word;animation:fade .6s .3s both}
            .specific.hint{border-left-color:#ffd166;font-family:inherit}.specific span{display:block;font:700 .7rem "Segoe UI",sans-serif;text-transform:uppercase;letter-spacing:1.4px;color:#cfe0ff;margin-bottom:3px}
            .ctx{display:grid;grid-template-columns:repeat(auto-fit,minmax(200px,1fr));gap:10px;margin:16px 0}
            .ctx div{background:rgba(255,255,255,.08);border-radius:12px;padding:10px 14px;animation:fade .6s .4s both}.ctx span{display:block;font-size:.7rem;text-transform:uppercase;letter-spacing:1.2px;color:#c3d3f3}.ctx b{word-break:break-all}
            h2{margin:6px 0 8px;font-size:1.15rem}
            .steps{list-style:none;margin:0;padding:0;display:grid;gap:8px}
            .steps li{display:flex;gap:12px;align-items:center;background:linear-gradient(90deg,rgba(255,255,255,.14),rgba(255,255,255,.05));border-radius:14px;padding:11px 14px;animation:slide .55s calc(.45s + var(--i)*.14s) both}
            .steps b{flex:0 0 32px;height:32px;border-radius:50%%;display:grid;place-items:center;background:linear-gradient(135deg,var(--c1),var(--c2));box-shadow:0 0 0 3px rgba(255,255,255,.25)}
            @keyframes slide{from{transform:translateX(-26px);opacity:0}}@keyframes fade{from{opacity:0;transform:translateY(8px)}}
            .idea{display:flex;gap:16px;align-items:center;margin:16px 0 4px;padding:15px 18px;border-radius:18px;background:linear-gradient(120deg,rgba(255,214,102,.28),rgba(255,255,255,.08));border:1px dashed #ffd166;animation:fade .7s .9s both}
            .idea h3{margin:0 0 3px;color:#ffe08a}.idea p{margin:0;line-height:1.5}.bulb{font-size:2.4rem;animation:glow 2s ease-in-out infinite}
            @keyframes glow{50%%{transform:scale(1.18);filter:drop-shadow(0 0 14px #ffd166)}}
            footer{margin-top:18px}.btns{display:flex;gap:10px;flex-wrap:wrap}
            button,.btn{font:700 .95rem "Segoe UI",sans-serif;color:#1a1033;background:linear-gradient(#fff,#e4ecff);border:0;border-radius:12px;padding:11px 18px;cursor:pointer;text-decoration:none;box-shadow:0 4px 0 rgba(8,14,40,.28);transition:transform .15s}
            button:hover,.btn:hover{transform:translateY(-3px)}button:active,.btn:active{transform:translateY(2px);box-shadow:none}
            .ref{margin-top:12px;font-size:.8rem;color:#c3d3f3}
            .a-spin{transform-origin:center;transform-box:fill-box;animation:spin 6s linear infinite}.a-spin2{transform-origin:center;transform-box:fill-box;animation:spin 4s linear infinite reverse}
            .a-float{animation:float 3s ease-in-out infinite}.a-shake{transform-origin:center;transform-box:fill-box;animation:shake 1.4s ease-in-out infinite}
            .a-pulse{transform-origin:center;transform-box:fill-box;animation:pulse 1.8s ease-in-out infinite}.a-move{animation:move 2.4s ease-in-out infinite alternate}
            .a-wave{animation:wave 1.6s ease-in-out infinite}
            @keyframes spin{to{transform:rotate(360deg)}}@keyframes float{50%%{transform:translateY(-10px)}}
            @keyframes shake{0%%,100%%{transform:rotate(0)}25%%{transform:rotate(-8deg)}75%%{transform:rotate(8deg)}}
            @keyframes pulse{50%%{transform:scale(1.14)}}@keyframes move{to{transform:translateX(18px)}}@keyframes wave{0%%{opacity:.9}100%%{opacity:0;transform:translateX(14px)}}
            @media (prefers-reduced-motion:reduce){*{animation-duration:.01ms!important;animation-iteration-count:1!important}}
            @media (max-width:640px){.card{padding:20px 16px}.scene{flex-basis:100%%;height:150px}}
            """.formatted(c1, c2);
    }

    /** Animated illustration for the category. */
    static String scene(String category) {
        String body = switch (category) {
            case "FILE" -> "<g class=\"a-float\"><path d=\"M52 14h62l34 34v94H52z\" fill=\"#fff\"/><path d=\"M114 14v34h34z\" fill=\"#cdd9ff\"/><rect x=\"66\" y=\"66\" width=\"68\" height=\"6\" rx=\"3\" fill=\"#b9c6ee\"/><rect x=\"66\" y=\"82\" width=\"52\" height=\"6\" rx=\"3\" fill=\"#b9c6ee\"/><circle cx=\"88\" cy=\"112\" r=\"4\" fill=\"#2b2b55\"/><circle cx=\"112\" cy=\"112\" r=\"4\" fill=\"#2b2b55\"/><path d=\"M86 128q14-12 28 0\" stroke=\"#2b2b55\" stroke-width=\"4\" fill=\"none\" stroke-linecap=\"round\"/></g>";
            case "NETWORK" -> "<g class=\"a-move\"><rect x=\"14\" y=\"64\" width=\"62\" height=\"34\" rx=\"10\" fill=\"#fff\"/><rect x=\"76\" y=\"72\" width=\"26\" height=\"6\" fill=\"#ffd166\"/><rect x=\"76\" y=\"84\" width=\"26\" height=\"6\" fill=\"#ffd166\"/></g><rect x=\"128\" y=\"58\" width=\"58\" height=\"46\" rx=\"10\" fill=\"#fff\"/><circle cx=\"150\" cy=\"81\" r=\"5\" fill=\"#2b2b55\"/><circle cx=\"168\" cy=\"81\" r=\"5\" fill=\"#2b2b55\"/><g class=\"a-pulse\"><path d=\"M112 30l-8 22h12l-8 22\" stroke=\"#ffe08a\" stroke-width=\"6\" fill=\"none\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/></g>";
            case "TRANSLATION" -> "<g class=\"a-float\"><path d=\"M24 28h86a12 12 0 0 1 12 12v40a12 12 0 0 1-12 12H66l-24 20V92H24a12 12 0 0 1-12-12V40a12 12 0 0 1 12-12z\" fill=\"#fff\"/><text x=\"58\" y=\"76\" font-size=\"38\" font-weight=\"800\" fill=\"#7b2ff7\" text-anchor=\"middle\">A</text></g><g class=\"a-float\" style=\"animation-delay:-1.2s\"><path d=\"M96 78h86a12 12 0 0 1 12 12v32a12 12 0 0 1-12 12h-18v18l-24-18H96a12 12 0 0 1-12-12V90a12 12 0 0 1 12-12z\" fill=\"#ffd166\"/><text x=\"140\" y=\"124\" font-size=\"34\" font-weight=\"800\" fill=\"#b4207a\" text-anchor=\"middle\">?</text></g>";
            case "VOICE" -> "<g class=\"a-pulse\"><path d=\"M30 66h26l34-26v90l-34-26H30z\" fill=\"#fff\"/></g><g stroke=\"#fff\" stroke-width=\"7\" stroke-linecap=\"round\" fill=\"none\"><path class=\"a-wave\" d=\"M110 60q16 20 0 40\"/><path class=\"a-wave\" style=\"animation-delay:-.5s\" d=\"M128 46q28 34 0 68\"/></g><g class=\"a-shake\"><path d=\"M146 118l32 32M178 118l-32 32\" stroke=\"#ffd166\" stroke-width=\"9\" stroke-linecap=\"round\"/></g>";
            case "SECURITY" -> "<g class=\"a-shake\"><rect x=\"54\" y=\"70\" width=\"92\" height=\"72\" rx=\"14\" fill=\"#fff\"/><path d=\"M72 70V54a28 28 0 0 1 56 0v16\" stroke=\"#fff\" stroke-width=\"12\" fill=\"none\"/><circle cx=\"100\" cy=\"98\" r=\"9\" fill=\"#cb2d3e\"/><rect x=\"96\" y=\"104\" width=\"8\" height=\"22\" rx=\"4\" fill=\"#cb2d3e\"/></g>";
            case "DATABASE" -> "<g class=\"a-float\"><ellipse cx=\"100\" cy=\"40\" rx=\"52\" ry=\"16\" fill=\"#fff\"/><path d=\"M48 40v30c0 9 23 16 52 16s52-7 52-16V40c0 9-23 16-52 16S48 49 48 40z\" fill=\"#d7e6ff\"/><path d=\"M48 70v30c0 9 23 16 52 16s52-7 52-16V70c0 9-23 16-52 16S48 79 48 70z\" fill=\"#fff\"/><path d=\"M48 100v26c0 9 23 16 52 16s52-7 52-16v-26c0 9-23 16-52 16s-52-7-52-16z\" fill=\"#d7e6ff\"/></g><g class=\"a-pulse\" fill=\"#ffe08a\" font-weight=\"800\"><text x=\"150\" y=\"36\" font-size=\"26\">z</text><text x=\"166\" y=\"20\" font-size=\"20\">z</text><text x=\"178\" y=\"8\" font-size=\"15\">z</text></g>";
            case "INPUT" -> "<g class=\"a-float\"><rect x=\"46\" y=\"22\" width=\"108\" height=\"128\" rx=\"14\" fill=\"#fff\"/><rect x=\"76\" y=\"12\" width=\"48\" height=\"22\" rx=\"8\" fill=\"#ffd166\"/><rect x=\"64\" y=\"60\" width=\"72\" height=\"8\" rx=\"4\" fill=\"#cfd8f5\"/><rect x=\"64\" y=\"80\" width=\"72\" height=\"8\" rx=\"4\" fill=\"#cfd8f5\"/><rect x=\"64\" y=\"100\" width=\"44\" height=\"8\" rx=\"4\" fill=\"#cfd8f5\"/></g><g class=\"a-shake\"><circle cx=\"150\" cy=\"128\" r=\"22\" fill=\"#ff6b6b\"/><rect x=\"146\" y=\"114\" width=\"8\" height=\"18\" rx=\"4\" fill=\"#fff\"/><circle cx=\"150\" cy=\"139\" r=\"4.5\" fill=\"#fff\"/></g>";
            case "NOTFOUND" -> "<text x=\"100\" y=\"96\" font-size=\"80\" font-weight=\"900\" fill=\"rgba(255,255,255,.88)\" text-anchor=\"middle\">?</text><g class=\"a-move\"><circle cx=\"70\" cy=\"72\" r=\"30\" fill=\"rgba(255,255,255,.25)\" stroke=\"#fff\" stroke-width=\"9\"/><path d=\"M92 96l34 34\" stroke=\"#fff\" stroke-width=\"12\" stroke-linecap=\"round\"/></g>";
            case "SERVER" -> "<g class=\"a-spin\"><circle cx=\"78\" cy=\"84\" r=\"30\" fill=\"#fff\"/><circle cx=\"78\" cy=\"84\" r=\"12\" fill=\"#b91d47\"/><g fill=\"#fff\"><rect x=\"72\" y=\"40\" width=\"12\" height=\"16\"/><rect x=\"72\" y=\"112\" width=\"12\" height=\"16\"/><rect x=\"34\" y=\"78\" width=\"16\" height=\"12\"/><rect x=\"106\" y=\"78\" width=\"16\" height=\"12\"/></g></g><g class=\"a-spin2\"><circle cx=\"142\" cy=\"58\" r=\"20\" fill=\"#ffd166\"/><circle cx=\"142\" cy=\"58\" r=\"8\" fill=\"#b91d47\"/><g fill=\"#ffd166\"><rect x=\"138\" y=\"28\" width=\"8\" height=\"11\"/><rect x=\"138\" y=\"77\" width=\"8\" height=\"11\"/><rect x=\"112\" y=\"54\" width=\"11\" height=\"8\"/><rect x=\"161\" y=\"54\" width=\"11\" height=\"8\"/></g></g>";
            case "SCHOOL" -> "<g class=\"a-float\"><rect x=\"48\" y=\"22\" width=\"104\" height=\"130\" rx=\"12\" fill=\"#fff\"/><rect x=\"76\" y=\"12\" width=\"48\" height=\"20\" rx=\"8\" fill=\"#38ef7d\"/><path d=\"M66 66l8 8 14-16\" stroke=\"#11998e\" stroke-width=\"6\" fill=\"none\" stroke-linecap=\"round\"/><rect x=\"98\" y=\"64\" width=\"38\" height=\"7\" rx=\"3\" fill=\"#cfeee6\"/><path d=\"M66 100l8 8 14-16\" stroke=\"#11998e\" stroke-width=\"6\" fill=\"none\" stroke-linecap=\"round\"/><rect x=\"98\" y=\"98\" width=\"30\" height=\"7\" rx=\"3\" fill=\"#cfeee6\"/></g><g class=\"a-pulse\"><circle cx=\"158\" cy=\"128\" r=\"22\" fill=\"#ffd166\"/><path d=\"M158 114v15l10 6\" stroke=\"#6b4b00\" stroke-width=\"5\" fill=\"none\" stroke-linecap=\"round\"/></g>";
            default -> "<g class=\"a-pulse\"><circle cx=\"100\" cy=\"80\" r=\"52\" fill=\"#fff\"/><rect x=\"94\" y=\"68\" width=\"12\" height=\"38\" rx=\"6\" fill=\"#2193b0\"/><circle cx=\"100\" cy=\"52\" r=\"7\" fill=\"#2193b0\"/></g>";
        };
        return "<svg viewBox=\"0 0 200 160\" xmlns=\"http://www.w3.org/2000/svg\" aria-hidden=\"true\">" + body + "</svg>";
    }
}
