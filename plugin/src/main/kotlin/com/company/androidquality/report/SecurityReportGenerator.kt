package com.company.androidquality.report

import io.gitlab.arturbosch.detekt.api.Finding
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object SecurityReportGenerator {

    fun generate(findings: List<Finding>, outputFile: File, projectName: String) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val html = buildHtml(findings, projectName, timestamp)
        outputFile.writeText(html)
    }

    private fun buildHtml(findings: List<Finding>, projectName: String, timestamp: String): String {
        val severityCounts = findings.groupBy { it.severity.name }
        val rowsHtml = if (findings.isEmpty()) {
            """<tr><td colspan="4" style="text-align:center;color:#28a745;font-weight:bold;">No violations found</td></tr>"""
        } else {
            findings.joinToString("\n") { finding ->
                val severityClass = finding.severity.name.lowercase()
                """        <tr class="severity-$severityClass">
          <td><code>${escape(finding.id)}</code></td>
          <td>${escape(finding.message)}</td>
          <td><code>${escape(finding.location.filePath.toString())}:${finding.location.source.line}</code></td>
          <td><span class="badge $severityClass">${escape(finding.severity.name)}</span></td>
        </tr>"""
            }
        }

        val summaryCards = buildSummaryCards(findings.size, severityCounts)

        return """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Security Report — $projectName</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #f8f9fa; color: #212529; }
    header { background: #1a1a2e; color: #fff; padding: 24px 32px; }
    header h1 { font-size: 1.5rem; font-weight: 600; }
    header p { margin-top: 4px; opacity: 0.7; font-size: 0.875rem; }
    .container { max-width: 1200px; margin: 0 auto; padding: 32px; }
    .summary { display: flex; gap: 16px; margin-bottom: 32px; flex-wrap: wrap; }
    .card { background: #fff; border-radius: 8px; padding: 20px 24px; flex: 1; min-width: 160px; box-shadow: 0 1px 3px rgba(0,0,0,.08); border-left: 4px solid #dee2e6; }
    .card.total { border-left-color: #6c757d; }
    .card.security { border-left-color: #dc3545; }
    .card.warning { border-left-color: #ffc107; }
    .card .number { font-size: 2rem; font-weight: 700; line-height: 1; }
    .card .label { font-size: 0.8rem; color: #6c757d; margin-top: 4px; text-transform: uppercase; letter-spacing: .05em; }
    table { width: 100%; border-collapse: collapse; background: #fff; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,.08); }
    th { background: #1a1a2e; color: #fff; padding: 12px 16px; text-align: left; font-size: 0.8rem; text-transform: uppercase; letter-spacing: .05em; }
    td { padding: 12px 16px; border-bottom: 1px solid #f0f0f0; font-size: 0.9rem; vertical-align: top; }
    tr:last-child td { border-bottom: none; }
    tr:hover td { background: #f8f9fa; }
    code { font-family: "SFMono-Regular", Consolas, monospace; font-size: 0.85em; background: #f1f3f5; padding: 2px 6px; border-radius: 3px; }
    .badge { display: inline-block; padding: 2px 8px; border-radius: 12px; font-size: 0.75rem; font-weight: 600; text-transform: uppercase; }
    .badge.security { background: #fde8e8; color: #c0392b; }
    .badge.warning { background: #fff3cd; color: #856404; }
    .badge.info { background: #d1ecf1; color: #0c5460; }
    .badge.minor { background: #e8f5e9; color: #2e7d32; }
    footer { text-align: center; padding: 24px; color: #6c757d; font-size: 0.8rem; }
  </style>
</head>
<body>
  <header>
    <h1>Security Scan Report — $projectName</h1>
    <p>Generated: $timestamp &nbsp;|&nbsp; Android Quality Plugin</p>
  </header>
  <div class="container">
    <div class="summary">
      $summaryCards
    </div>
    <table>
      <thead>
        <tr>
          <th>Rule</th>
          <th>Description</th>
          <th>Location</th>
          <th>Severity</th>
        </tr>
      </thead>
      <tbody>
$rowsHtml
      </tbody>
    </table>
  </div>
  <footer>Android Quality Plugin &mdash; OWASP MASVS Security Scan</footer>
</body>
</html>"""
    }

    private fun buildSummaryCards(total: Int, severityCounts: Map<String, List<Finding>>): String {
        val security = severityCounts["Security"]?.size ?: 0
        val warning = severityCounts["Warning"]?.size ?: 0
        return """<div class="card total">
        <div class="number">$total</div>
        <div class="label">Total Violations</div>
      </div>
      <div class="card security">
        <div class="number">$security</div>
        <div class="label">Security</div>
      </div>
      <div class="card warning">
        <div class="number">$warning</div>
        <div class="label">Warning</div>
      </div>"""
    }

    private fun escape(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
