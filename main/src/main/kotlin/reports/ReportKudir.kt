package reports

import com.github.jhonnymertz.wkhtmltopdf.wrapper.Pdf
import com.github.jhonnymertz.wkhtmltopdf.wrapper.params.Param
import db.DBManager
import db.OrientDatabase
import i18n.t
import models.SpendingTypes
import org.json.JSONArray
import org.json.JSONObject
import system.ConfigManager
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

/**
 * Class used to generate KUDiR report
 */
class ReportKudir: BaseReport() {

    /**
     * Method used to request data, required for report from database
     * @param options: Query parameters
     * @return JSON object with data or empty JSON object if could not get data
     */
    override fun getData(options:HashMap<String,String>):JSONObject {
        var result = JSONObject()
        val companyRid = options["companyRid"] ?: return result
        val period = options["period"] ?: return result
        if (companyRid.isEmpty() || period.isEmpty()) return result
        val db = DBManager.getDB() as OrientDatabase
        val response = db.execQueryJSON("/getReportData_kudir/$companyRid/$period",hashMapOf("function" to true))
                ?: return result
        if (!response.has("result")) return result
        val arr = response["result"] as? JSONArray ?: return result
        result = arr[0] as? JSONObject ?: return result
        return result
    }

    /**
     * Method used to generate report as HTML and return as a astring
     * @param companyRid: ID of company
     * @param period: Date in period of report
     * @return generated report HTML
     */
    override fun generate(companyRid: String, period: Long): String {
        val data = getData(hashMapOf("companyRid" to companyRid, "period" to period.toString()))
        if (!data.has("incomes") || !data.has("spendings") || !data.has("header")) {
            return "<h1 style='color:red'>" + t("Ошибка данных") + "</h1>"
        }
        val page_break = """<P style="page-break-before: always"></P>"""
        return renderCommon() + renderHeader(data["header"] as JSONObject) + page_break +
                renderIncomes(data["incomes"] as JSONObject) + page_break +
                renderSpendings(data["spendings"] as JSONObject)
    }

    /**
     * Method used to generate report as PDF
     * @param companyRid: ID of company
     * @param period: Date in period of report
     * @return String with file name of resulting PDF file or null in case of error
     */
    override fun generatePdf(companyRid: String, period: Long,token:String): String {
        // Get data for report
        val data = getData(hashMapOf("companyRid" to companyRid, "period" to period.toString()))
        if (!data.has("incomes") || !data.has("spendings") || !data.has("header")) {
            return ""
        }
        val prefix = this.prepareRequestPath()
        val requestId = prefix.split("/").last()
        val reportFileName = "${ConfigManager.webConfig["cache_path"].toString()}/$requestId.pdf"
        // Create report HTML for each section
        val common = this.renderCommon()
        val header = this.renderHeader(data["header"] as JSONObject)
        val incomes = this.renderIncomes(data["incomes"] as JSONObject)
        val spendings = this.renderSpendings(data["spendings"] as JSONObject)
        // Write sections to separate PDF files
        var pdf = Pdf()
        pdf.addPageFromString(common+header)
        pdf.saveAs("$prefix/kudir_header.pdf")
        pdf = Pdf()
        pdf.addPageFromString(common+incomes)
        pdf.saveAs("$prefix/kudir_incomes.pdf")
        pdf = Pdf()
        pdf.addPageFromString(common+spendings)
        pdf.addParam(Param("-O ","landscape"))
        pdf.saveAs("$prefix/kudir_spendings.pdf")

        // Concatenate produced PDF files to single one
        Runtime.getRuntime().exec("pdfunite $prefix/kudir_header.pdf $prefix/kudir_incomes.pdf $prefix/kudir_spendings.pdf $reportFileName")
        Thread.sleep(2000)
        // Delete temporary files
        cleanTemporaryFiles(prefix)
        // Return filename of generated report
        return reportFileName
    }

    /**
     * Method used to generate common header on top of report HTML
     * @return generated HTML Code
     */
    private fun renderCommon():String {
        val styles = BufferedReader(InputStreamReader(this.javaClass.getResourceAsStream("/kudir.css"))).readText()
        return  """
            <html xmlns="http://www.w3.org/1999/xhtml">
            <head profile="http://dublincore.org/documents/dcmi-terms/">
            <meta http-equiv="Content-Type" content="application/xhtml+xml; charset=utf-8"/>
            <title xml:lang="en-US"></title>
            <link rel="schema.DC" href="http://purl.org/dc/elements/1.1/" hreflang="en"/>
            <link rel="schema.DCTERMS" href="http://purl.org/dc/terms/" hreflang="en"/>
            <link rel="schema.DCTYPE" href="http://purl.org/dc/dcmitype/" hreflang="en"/>
            <link rel="schema.DCAM" href="http://purl.org/dc/dcam/" hreflang="en"/>
            <style>$styles</style>
            </head>
            <body dir="ltr" style="margin-top:0.5909in; margin-bottom:0.5909in;
            margin-left:0.3in; margin-right:0.3in;height:100%">
            <style>
                h1.header {
                    font-familty:Times New Roman;
                    font-weight:bold;
                    font-size:14px;
                }
                table.inner td {
                    padding:3px;
                }
                table.inner {
                    border-width:1px;
                    border-color:black;
                    border-style:solid;
                    border-collapse:collapse;
                    width:100%;
                }
                @viewport {
                    orientation: landscape
                }
            </style>
        """
    }

    /**
     * Method used to generate header page of report
     * @param data: Data needed to generate this part of report
     * @returns HTML string
     */
    private fun renderHeader(data:JSONObject):String {
        val name = data["name"]
        val address = data["address"]
        val bank_accounts = data["bank_accounts"].toString()
        val year = data["year"].toString()
        val inn = data["inn"].toString()
        val kpp = data["kpp"].toString()

        var inn_block = ""
        var innkpp_block = ""
        var innkpp_symbol: String
        var inn_symbol: String

        for (i in 0..21) {
            if (kpp.isEmpty()) {
                inn_symbol = if (i<inn.length) inn[i].toString() else ""
                innkpp_symbol = if (i==12) "/" else ""
            } else {
                inn_symbol = ""
                innkpp_symbol = if (i<12) inn[i].toString() else if (i==12) "/" else if (i-13<kpp.length) kpp[i-13].toString() else ""
            }
            if (i<12) {
                inn_block += """
                        <td style="text-align:left;width:0.2215in; " class="Таблица1_A11"><p class="P19">$inn_symbol</p></td>
                    """
            }
            innkpp_block += """
                    <td style="text-align:left;width:0.2215in; " class="Таблица1_A11"><p class="P19">$innkpp_symbol</p></td>
                """
        }
        inn_block += """
            <td colspan="23" style="text-align:left;width:0.066in; " class="Таблица1_e11"><p class="P17"> </p></td>
            <td colspan="3" style="text-align:left;width:0.2958in; " class="Таблица1_j4"><p class="P19"> </p></td>
        """
        innkpp_block += """
            <td colspan="13" style="text-align:left;width:0.066in; " class="Таблица1_e11"><p class="P17"> </p></td>
            <td colspan="3" style="text-align:left;width:0.2958in; " class="Таблица1_j4"><p class="P19"> </p></td>
        """

        return """
            <p class="P42">Приложение N 1</p>
            <p class="P41">к приказу Минфина России</p>
            <p class="P41">от 22.10.2012 N 135н (в редакции</p>
            <p class="P41">приказа Минфина России</p>
            <p class="P41">от 07.12.2016 N 227н)</p>
            <p class="P41"> </p>
            <p class="P15"> </p>
            <p class="P16">КНИГА</p>
            <p class="P4">учета доходов и расходов организаций и индивидуальных предпринимателей,</p><h5 class="P14"><a
                    id="a__применяющих_упрощенную_систему_налогообложения"><span/></a>применяющих упрощенную систему налогообложения
            </h5>
            <p class="P1"> </p>
            <p class="P1"> </p>
            <table border="0" cellspacing="0" cellpadding="0" style="width:100%;height:100%">
                <colgroup>
                    <col width="25"/><col width="25"/><col width="25"/><col width="25"/><col width="25"/><col width="10"/>
                    <col width="11"/><col width="3"/><col width="19"/><col width="6"/><col width="5"/><col width="20"/>
                    <col width="2"/><col width="23"/><col width="25"/><col width="8"/><col width="17"/><col width="25"/>
                    <col width="25"/><col width="25"/><col width="25"/><col width="25"/><col width="25"/><col width="10"/>
                    <col width="14"/><col width="7"/><col width="17"/><col width="25"/><col width="25"/>
                    <col width="25"/><col width="7"/><col width="3"/><col width="66"/><col width="2"/>
                    <col width="6"/><col width="33"/><col width="33"/><col width="34"/>
                </colgroup>

                <tr class="Таблица11">
                    <td colspan="33" style="text-align:left;width:0.2215in; " class="Таблица1_A1"><p class="P17"> </p></td>
                    <td colspan="2" style="text-align:left;width:0.0174in; " class="Таблица1_A1"><p class="P17"> </p></td>
                    <td colspan="3" style="text-align:left;width:0.2958in; " class="Таблица1_j1"><p class="P18">Коды</p></td>
                </tr>
                <tr class="Таблица11">
                    <td colspan="24" style="text-align:left;width:0.2215in; " class="Таблица1_A1"><p class="P17"> </p></td>
                    <td colspan="9" style="text-align:left;width:0.1299in; " class="Таблица1_A1"><p class="P20">Форма по ОКУД</p>
                    </td>
                    <td colspan="2" style="text-align:left;width:0.0174in; " class="Таблица1_A1"><p class="P17"> </p></td>
                    <td colspan="3" style="text-align:left;width:0.2958in; " class="Таблица1_j2"><p class="P19"> </p></td>
                </tr>
                <tr class="Таблица11">
                    <td colspan="17" style="text-align:left;width:0.2215in; " class="Таблица1_A1"><p class="P17"> </p></td>
                    <td colspan="16" style="text-align:left;width:0.1299in; " class="Таблица1_A1"><p class="P20">Дата (год, месяц,
                        число)</p></td>
                    <td colspan="2" style="text-align:left;width:0.0174in; " class="Таблица1_A1"><p class="P17"> </p></td>
                    <td style="text-align:left;width:0.2958in; " class="Таблица1_j3"><p class="P19"> </p></td>
                    <td style="text-align:left;width:0.2958in; " class="Таблица1_j3"><p class="P19"> </p></td>
                    <td style="text-align:left;width:0.3056in; " class="Таблица1_j1"><p class="P19"> </p></td>
                </tr>

                <tr class="Таблица14">
                    <td colspan="6" style="text-align:left;width:0.2215in; " class="Таблица1_A1"><p class="P20">на </p></td>
                    <td colspan="3" style="text-align:left;width:0.0986in; " class="Таблица1_G4"><p class="P19">$year</p></td>
                    <td colspan="24" style="text-align:left;width:0.0542in; " class="Таблица1_A1"><p class="таблица">год</p></td>
                    <td colspan="2" style="text-align:left;width:0.0174in; " class="Таблица1_A1"><p class="P17"> </p></td>
                    <td colspan="3" style="text-align:left;width:0.2958in; " class="Таблица1_j4"><p class="P19"> </p></td>
                </tr>
                <tr class="Таблица11">
                    <td colspan="33" style="text-align:left;width:0.2215in; " class="Таблица1_A1"><p class="P17"> </p></td>
                    <td colspan="2" style="text-align:left;width:0.0174in; " class="Таблица1_A1"><p class="P17"> </p></td>
                    <td colspan="3" style="text-align:left;width:0.2958in; " class="Таблица1_j4"><p class="P19"> </p></td>
                </tr>
                <tr class="Таблица16">
                    <td colspan="15" style="text-align:left;width:0.2215in; " class="Таблица1_A1"><p class="таблица">
                        Налогоплательщик (наименование организации/фамилия, имя, отчество индивидуального
                        предпринимателя)</p></td>
                    <td colspan="16" style="text-align:left"><p class="P19">$name</p></td>
                    <td colspan="4" style="text-align:left;width:0.0257in; " class="Таблица1_A1"><p class="P17"> </p></td>
                    <td colspan="3" style="text-align:left;width:0.2958in; " class="Таблица1_j4"><p class="P19"> </p></td>
                </tr>

                <tr class="Таблица16">
                    <td colspan="16" style="text-align:left;width:0.2215in; " class="Таблица1_A1"><p class="таблица"></p></td>
                    <td colspan="19" style="text-align:left;width:0.0257in; " class="Таблица1_A1"><p class="P17"> </p></td>
                    <td colspan="3" style="text-align:left;width:0.2958in; " class="Таблица1_j4"><p class="P19"> </p></td>
                </tr>
                <tr class="Таблица16">
                    <td colspan="31" style="text-align:left;width:0.2215in; " class="Таблица1_A1"><p class="P17"> </p></td>
                    <td colspan="3" style="text-align:left;width:0.0257in; " class="Таблица1_A1"><p class="P20">по ОКПО</p></td>
                    <td style="text-align:left;width:0.0528in; " class="Таблица1_A1"><p class="P17"> </p></td>
                    <td colspan="3" style="text-align:left;width:0.2958in; " class="Таблица1_j4"><p class="P19"> </p></td>
                </tr>
                <tr class="Таблица16">
                    <td colspan="35" style="text-align:left;width:0.2215in; " class="Таблица1_A1"><p class="P17"> </p></td>
                    <td colspan="3" style="text-align:left;width:0.2958in; " class="Таблица1_j4"><p class="P19"> </p></td>
                </tr>
                <tr class="Таблица16">
                    <td colspan="35" style="text-align:left;width:0.2215in; " class="Таблица1_A1"><p class="таблица">
                        Идентификационный номер налогоплательщика - организации/код причины постановки на учет в налоговом органе
                        (ИНН/КПП)</p></td>
                    <td colspan="3" style="text-align:left;width:0.2958in; " class="Таблица1_j4"><p class="P19"> </p></td>
                </tr>
                <tr class="Таблица11">
                $innkpp_block
                </tr>
                <tr class="Таблица16">
                    <td colspan="35" style="text-align:left;width:0.2215in; " class="Таблица1_A1"><p class="P17"> </p></td>
                    <td colspan="3" style="text-align:left;width:0.2958in; " class="Таблица1_j4"><p class="P19"> </p></td>
                </tr>
                <tr class="Таблица16">
                    <td colspan="35" style="text-align:left;width:0.2215in; " class="Таблица1_A1"><p class="таблица">
                        Идентификационный номер налогоплательщика - индивидуального предпринимателя (ИНН)</p>
                        <p class="таблица"> </p></td>
                    <td colspan="3" style="text-align:left;width:0.2958in; " class="Таблица1_j4"><p class="P19"> </p></td>
                </tr>
                <tr class="Таблица11">
                $inn_block
                </tr>
                <tr class="Таблица16">
                    <td colspan="35" style="text-align:left;width:0.2215in; " class="Таблица1_A1"><p class="P17"> </p></td>
                    <td colspan="3" style="text-align:left;width:0.2958in; " class="Таблица1_j4"><p class="P19"> </p></td>
                </tr>
                <tr class="Таблица16">
                    <td colspan="11" style="text-align:left;width:0.2215in; " class="Таблица1_A1"><p class="таблица">Объект
                        налогообложения</p></td>
                    <td colspan="20" style="text-align:left;width:0.1785in; " class="Таблица1_G4"><p class="P19">доходы</p></td>
                    <td colspan="4" style="text-align:left;width:0.0257in; " class="Таблица1_A1"><p class="P17"> </p></td>
                    <td colspan="3" style="text-align:left;width:0.2958in; " class="Таблица1_j4"><p class="P19"> </p></td>
                </tr>
                <tr class="Таблица16">
                    <td colspan="11" style="text-align:left;width:0.2215in; " class="Таблица1_A1"><p class="P17"> </p></td>
                    <td colspan="20" style="text-align:left;width:0.1785in; " class="Таблица1_L17"><p class="P22">(наименование
                        выбранного объекта налогообложения</p></td>
                    <td colspan="4" style="text-align:left;width:0.0257in; " class="Таблица1_L17"><p class="P23"> </p></td>
                    <td colspan="3" style="text-align:left;width:0.2958in; " class="Таблица1_j4"><p class="P23"> </p></td>
                </tr>
                <tr class="Таблица16">
                    <td colspan="35" style="text-align:left;width:0.2215in; " class="Таблица1_G4"><p class="P24"> </p></td>
                    <td colspan="3" style="text-align:left;width:0.2958in; " class="Таблица1_j4"><p class="P23"> </p></td>
                </tr>
                <tr class="Таблица16">
                    <td colspan="35" style="text-align:left;width:0.2215in; " class="Таблица1_L17"><p class="P22">в соответствии со
                        статьей 346.14 Налогового кодекса Российской Федерации)</p></td>
                    <td colspan="3" style="text-align:left;width:0.2958in; " class="Таблица1_j4"><p class="P23"> </p></td>
                </tr>
                <tr class="Таблица16">
                    <td colspan="32" style="text-align:left;width:0.2215in; " class="Таблица1_A1"><p class="P24"> </p></td>
                    <td colspan="3" style="text-align:left;width:0.5931in; " class="Таблица1_A1"><p class="P24"> </p></td>
                    <td colspan="3" style="text-align:left;width:0.2958in; " class="Таблица1_j20"><p class="P19"> </p></td>
                </tr>
                <tr class="Таблица11">
                    <td colspan="13" style="text-align:left;width:0.2215in; " class="Таблица1_A1"><p class="таблица">Единица
                        измерения: руб. </p></td>
                    <td colspan="19" style="text-align:left;width:0.2028in; " class="Таблица1_A1"><p class="P17"> </p></td>
                    <td colspan="3" style="text-align:left;width:0.5931in; " class="Таблица1_A1"><p class="таблица">по ОКЕИ</p></td>
                    <td colspan="3" style="text-align:left;width:0.2958in; " class="Таблица1_j20"><p class="P18">383</p></td>
                </tr>
                <tr class="Таблица16">
                    <td colspan="38" style="text-align:left;width:0.2215in; " class="Таблица1_A1"><p class="P17"> </p></td>
                </tr>
                <tr class="Таблица16">
                    <td colspan="38" style="text-align:left;width:0.2215in; " class="Таблица1_A1"><p class="таблица">Адрес места
                        нахождения организации</p>
                        <p class="таблица">(места жительства индивидуального </p></td>
                </tr>
                <tr class="Таблица16">
                    <td colspan="7" style="text-align:left;width:0.2215in; " class="Таблица1_A1"><p class="таблица">
                        предпринимателя)</p></td>
                    <td colspan="31" style="text-align:left;width:0.0299in; " class="Таблица1_G4"><p class="P19">$address</p></td>
                </tr>
                <tr class="Таблица16">
                    <td colspan="38" style="text-align:left;width:0.2215in; " class="Таблица1_G4"><p class="P19"> </p></td>
                </tr>
                <tr class="Таблица16">
                    <td colspan="18" style="text-align:left" class="Таблица1_A1"><p class="таблица">
                    Номера расчетных и иных счетов, открытых в учреждениях банков</p></td>
                    <td colspan="30" style="text-align:left" class="Таблица1_A1">
                        <p class="P19">
                            ${if (bank_accounts.length>28) bank_accounts.substring(0,28) else bank_accounts}
                        </p></td>
                </tr>
                <tr class="Таблица16">
                    <td colspan="18" style="text-align:left;width:0.2215in; " class="Таблица1_A1"><p class="P17"> </p></td>
                    <td colspan="30" style="text-align:left;width:0.1549in; " class="Таблица1_a27"><p class="P22">(номера расчетных
                        и</p></td>
                </tr>
                <tr class="Таблица16">
                    <td colspan="38" style="text-align:left;width:0.2215in; " class="Таблица1_G4">
                        <p style="font-size:10pt;font-family:Times New Roman;text-align:left">
                            ${if (bank_accounts.length>28) bank_accounts.substring(28) else ""}
                        </p>
                    </td>
                </tr>
                <tr class="Таблица16">
                    <td colspan="38" style="text-align:left;width:0.2215in; " class="Таблица1_L17"><p class="P22">иных счетов и
                        наименование соответствующих банков)</p></td>
                </tr>
                <tr class="Таблица16">
                    <td colspan="38" style="text-align:left;width:0.2215in; " class="Таблица1_A1"><p class="P23"> </p></td>
                </tr>
                <tr class="Таблица16">
                    <td colspan="38" style="text-align:left;width:0.2215in; " class="Таблица1_G4"><p class="P23"> </p></td>
                </tr>
                <tr class="Таблица132">
                    <td colspan="38" style="text-align:left;width:0.2215in; " class="Таблица1_G4"><p class="P23"> </p></td>
                </tr>
                <tr class="Таблица16">
                    <td colspan="38" style="text-align:left;width:0.2215in; " class="Таблица1_A1"><p class="P17"> </p></td>
                </tr>
            </table>
            """
    }

    /**
     * Method used to generate incomes part of report
     * @param data: Data needed to generate this part of report
     * @returns HTML string
     */
    private fun renderIncomes(data:JSONObject):String {
        var result = ""
        for (quarter in data.keys()) {
            result += renderIncomeQuarter(quarter.toString().toInt(),data[quarter] as JSONObject)
        }

        return result
    }

    /**
     * Method used to generate One quarter of incomes for report.
     * @param quarter: Quarter number
     * @param data: Data needed to generate this part of report
     * @returns HTML string
     */
    private fun renderIncomeQuarter(quarter:Int,data:JSONObject):String {
        var result = """
        <div align="center" style="padding-top:10px;padding-bottom:10px">
            <h1 class="header">I. Доходы и расходы</h1>
        </div>
        <table class="inner" border="1" cellspacing="0" cellpadding="0">
            <tr>
                <td colspan="3"><div align="center">Регистрация</div></td>
                <td colspan="2"><div align="center">Сумма</div></td>
            </tr>
            <tr>
                <td><div align="center">N п/п</div></td>
                <td><div align="center">Дата и номер первичного документа</div></td>
                <td><div align="center">Содержание операции</div></td>
                <td><div align="center">Доходы, учитываемые при исчислении налоговой базы</div></td>
                <td><div align="center">Расходы, учитываемые при исчислении налоговой базы</div></td>
            </tr>
            <tr>
                <td><div align="center">1</div></td>
                <td><div align="center">2</div></td>
                <td><div align="center">3</div></td>
                <td><div align="center">4</div></td>
                <td><div align="center">5</div></td>
            </tr>
        """.trimMargin()
        if (!data.has("rows")) return result
        val rows = data["rows"] as? JSONArray ?: return result
        var row_index = 1
        for (row_obj in rows) {
            val row  = row_obj as? JSONObject ?: continue
            result += """
                <tr><td><div align="center">$row_index</div></td>
                <td>${row["number_date"]}</td>
                <td>${row["description"]}</td>
                <td><div align="center">${row["amount"]}</div></td>
                <td></td></tr>""".trimMargin()
            row_index += 1
        }

        val footer_texts = getFooterLabels(quarter)

        result += """<tr>
            |<td colspan="3">${footer_texts["quarter_text"]}</td>
            |<td><div align="center">${data["quarter_amount"]}</div></td>
            |<td></td></tr>""".trimMargin()
        if (!footer_texts["total_text"]!!.isEmpty()) {
            result += """<tr>
                |<td colspan="3">${footer_texts["total_text"]}</td>
                |<td><div align="center">${data["total_amount"]}</div></td>
                |<td></td></tr>""".trimMargin()
        }
        result += """</table>"""
        if (quarter<4) {
            result += """<P style="page-break-before: always"></P>"""
        }
        return result
    }

    /**
     * Method used to generate spendings part of report
     * @param data: Data needed to generate this part of report
     * @returns HTML string
     */
    private fun renderSpendings(data:JSONObject):String {
        var result = """
            <div align="center" style="padding-bottom:10px;padding-top:10px">
                <h1 class="header">
                    IV. Расходы, предусмотренные пунктом 3.1 статьи 346.21 Налогового кодекса Российской Федерации,
                    уменьшающие сумму налога,уплачиваемого в связи с применением упрощенной системы налогообложения
                    (авансовых платежей по налогу)
                </h1>
                <h1 class="header">
                    за _______________ 20__ год
                </h1>
            </div>
            <table class="inner" border="1" cellspacing="0" cellpadding="0">
                <tr>
                    <td rowspan="2" style="white-space:nowrap"><div align="center">N п/п</div></td>
                    <td rowspan="2"><div align="center">Дата и номер первичного документа</div></td>
                    <td rowspan="2"><div align="center">Период, за который
                        произведена уплата страховых взносов, выплата пособия по временной нетрудоспособности,
                        предусмотренных в графах 4-9</div>
                    </td>
                    <td colspan="6"><div align="center">Сумма</div></td>
                    <td rowspan="2"><div align="center">Итого (руб.)</div></td>
                </tr>
                <tr>
                    <td>
                        <div align="center">Страховые взносы на обязательное пенсионное страхование (руб.)</div>
                    </td>
                    <td>
                        <div align="center">Страховые взносы на обязательное социальное страхование на случай
                        временной нетрудоспособности и в связи с материнством (руб.)</div>
                    </td>
                    <td>
                        <div align="center">Страховые взносы на обязательное медицинское страхование (руб.)</div>
                    </td>
                    <td>
                        <div align="center">Страховые взносы на обязательное социальное страхование от несчастных
                        случаев на производстве и профессиональных заболеваний (руб.)</div>
                    </td>
                    <td>
                        <div align="center">Расходы по выплате пособия по временной нетрудоспособности (руб.)</div>
                    </td>
                    <td>
                        <div align="center">Платежи (взносы) по договорам добровольного личного страхования (руб.)
                        </div>
                    </td>
                </tr>
                <tr>
                    <td><div align="center">1</div></td>
                    <td><div align="center">2</div></td>
                    <td><div align="center">3</div></td>
                    <td><div align="center">4</div></td>
                    <td><div align="center">5</div></td>
                    <td><div align="center">6</div></td>
                    <td><div align="center">7</div></td>
                    <td><div align="center">8</div></td>
                    <td><div align="center">9</div></td>
                    <td><div align="center">10</div></td>
                </tr>
        """
        for (quarter in data.keys()) {
            val quarter_data = data[quarter] as? JSONObject ?: continue
            result += renderSpending(quarter.toInt(),quarter_data)
        }
        return result
    }

    /**
     * Method used to generate Spendings for single quarter
     * @param quarter: Number of quarter
     * @param data: Data needed to generate this part of report
     * @returns HTML string
     */
    private fun renderSpending(quarter:Int,data:JSONObject): String {
        var result = ""
        val rows = data["rows"] as? JSONArray ?: return result
        var col_index = 1
        for (row_obj in rows) {
            val row = row_obj as? JSONObject ?: continue
            result += """<tr><td><div align="center">$col_index</div></td>"""
            result += """<td><div align="center">${row["number_date"]}</div></td>"""
            result += """<td><div align="center">${row["period"]}</div></td>"""
            var total_amount = ""
            for (spendingType in SpendingTypes.keys) {
                if (spendingType < 3) continue
                val amount = if (row.has("amount_$spendingType") &&
                        row["amount_$spendingType"].toString().toDouble()>0) row["amount_$spendingType"].toString() else ""
                result += """<td><div align="center">$amount</div></td>"""
                if (!amount.isEmpty()) {
                    total_amount = amount
                }
            }
            result += """<td><div align="center">$total_amount</div></td>"""
            result += "</tr>"
            col_index += 1
        }
        val footer_texts = getFooterLabels(quarter)
        result += """
            <tr>
                <td colspan="3">${footer_texts["quarter_text"]}</td>
        """
        for (spendingType in SpendingTypes.keys) {
            if (spendingType < 3) continue
            val amount = if (data.has("quarter_amount_$spendingType") &&
                    data["quarter_amount_$spendingType"].toString().toDouble()>0)
                data["quarter_amount_$spendingType"].toString() else ""
            result += """<td><div align="center">$amount</div></td>"""
        }
        result += """<td><div align="center">${data["quarter_amount"]}</div></td>"""
        if (!footer_texts["total_text"]!!.isEmpty()) {
            result += """
                <tr>
                    <td colspan="3">${footer_texts["total_text"]}</td>
            """
            for (spendingType in SpendingTypes.keys) {
                if (spendingType < 3) continue
                val amount = if (data.has("total_amount_$spendingType")
                        && data["total_amount_$spendingType"].toString().toDouble()>0)
                    data["total_amount_$spendingType"].toString() else ""
                result += """<td><div align="center">$amount</div></td>"""
            }
            result += """<td><div align="center">${data["total_amount"]}</div></td>"""
        }
        return result
    }

    /**
     * Utility function which returns texts for report table footers dependings on quarter
     * @param quarter: Quarter number
     * @return : Hashmap with items "quarter_text" and "total_text" with appropriate values
     */
    private fun getFooterLabels(quarter:Int): HashMap<String,String> {
        when (quarter) {
            1 -> {
                return hashMapOf("quarter_text" to "Итого за I квартал","total_text" to "")
            }
            2 -> {
                return hashMapOf(
                        "quarter_text" to "Итого за II квартал",
                        "total_text" to "Итого за полугодие"
                )
            }
            3 -> {
                return hashMapOf(
                        "quarter_text" to "Итого за III квартал",
                        "total_text" to "Итого за 9 месяцев"
                )
            }
            4 -> {
                return hashMapOf(
                    "quarter_text" to "Итого за IV квартал",
                    "total_text" to "Итого за год"
                )
            }
            else -> return hashMapOf("quarter_text" to "","total_text" to "")
        }
    }

    /**
     * Utility function used to remove all temporary files, generated during PDF production
     * @param prefix: Prefix of generated report (includes company ID and report name)
     */
    private fun cleanTemporaryFiles(prefix:String) {
        val files = arrayOf("$prefix/kudir_header.html","$prefix/kudir_incomes.html","$prefix/kudir_spendings.html",
                "$prefix/kudir_header.pdf","$prefix/kudir_incomes.pdf","$prefix/kudir_spendings.pdf","$prefix/lockfile")
        for (file in files) {
            if (Files.exists(Paths.get(file))) {
                Files.delete(Paths.get(file))
            }
        }
    }
}