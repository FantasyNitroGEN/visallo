package org.visallo.example.insiderThreat;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.vertexium.type.GeoPoint;
import org.visallo.core.util.VisalloDate;
import org.visallo.core.util.VisalloDateTime;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.*;

public class CreateInsiderThreadRdf {
    private static final String IRI_CONCEPT_TYPE_APPLICATION = "http://visallo.org/insider-threat#application";
    private static final String IRI_PROPERTY_ASSET_TITLE = "http://visallo.org/insider-threat#assetTitle";
    public static final String IRI_CONCEPT_TYPE_EMPLOYEE = "http://visallo.org/insider-threat#employee";
    public static final String IRI_PROPERTY_FIRST_NAME = "http://visallo.org/insider-threat#firstName[HR]";
    public static final String IRI_PROPERTY_LAST_NAME = "http://visallo.org/insider-threat#lastName[HR]";
    public static final String IRI_CONCEPT_TYPE_401K_TRANSACTION = "http://visallo.org/insider-threat#401kTransaction";
    public static final String IRI_PROPERTY_RISK = "http://visallo.org/insider-threat#risk";
    public static final String IRI_PROPERTY_RISK_REASON = "http://visallo.org/insider-threat#riskReason";
    public static final String APP_ACCOUNTING = "app-accounting";
    public static final String APP_INVENTORY = "app-inventory";
    public static final String APP_WIKI = "app-wiki";
    public static final String APP_HR = "app-hr";
    public static final String APP_WEB_SITE = "app-website";
    public static final String TITLE_BUSINESS_ANALYST = "BA";
    public static final String TITLE_MANAGER = "MGR";
    public static final String TITLE_SYSTEM_ADMINISTRATOR = "SA";
    public static final String TITLE_DEVELOPER = "DEV";
    public static final String PERFORMANCE_REVIEW_ACTION_REQUIRED = "AR";
    public static final String REMOTE_ACCESS_IP_ADDRESS = "216.169.133.23";

    @Parameter(names = {"--out", "-o"}, required = true, description = "Output file name")
    private File outputFile;

    @Parameter(names = {"--help", "-h"})
    private boolean help;

    public static void main(String[] args) throws Exception {
        new CreateInsiderThreadRdf().run(args);
    }

    private void run(String[] args) throws Exception {
        JCommander cmd = new JCommander(this, args);
        if (help) {
            cmd.usage();
            return;
        }

        outputFile.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            PrintWriter writer = new PrintWriter(out);
            RdfWriter rdfWriter = new RdfWriter(writer);
            writeApplications(rdfWriter);
            writeInsiderThreatEmployee(rdfWriter);
            writeNoiseEmployees(rdfWriter);
            writeManagerRelationships(rdfWriter);
            rdfWriter.close();
        }
    }

    private void writeManagerRelationships(RdfWriter out) {
        writeManagerRelationship(out, "employee-0001", "employee-0002");
        writeManagerRelationship(out, "employee-0003", "employee-0002");
        writeManagerRelationship(out, "employee-0004", "employee-0002");
    }

    private void writeManagerRelationship(RdfWriter out, String employeeVertexId, String managerVertexId) {
        out.writeRelationship(employeeVertexId, "http://visallo.org/insider-threat#hasManager", managerVertexId);
    }

    private void writeNoiseEmployees(RdfWriter out) {
        String employeeVertexId = "employee-0002";
        EmployeeRiskScores riskScores = new EmployeeRiskScores();
        riskScores.appAccounting = 0.2;
        riskScores.appInventory = 0.0;
        riskScores.appWiki = 0.0;
        riskScores.appHr = 0.0;
        riskScores.appWebsite = 0.1;
        riskScores.event401K = 0.1;
        riskScores.eventRemoteAccess = 0.2;
        writeEmployee(out, employeeVertexId, "KSF983", TITLE_MANAGER, "Garry", "Vasquez", riskScores, null);
        writePrivilege(out, employeeVertexId, APP_ACCOUNTING, "Accounting", EnumSet.of(AppPrivilege.READ, AppPrivilege.WRITE));

        employeeVertexId = "employee-0003";
        riskScores = new EmployeeRiskScores();
        riskScores.appAccounting = 0.2;
        riskScores.appInventory = 0.0;
        riskScores.appWiki = 0.2;
        riskScores.appHr = 0.0;
        riskScores.appWebsite = 0.1;
        riskScores.event401K = 0.0;
        riskScores.eventRemoteAccess = 0.2;
        writeEmployee(out, employeeVertexId, "UFJ234", TITLE_SYSTEM_ADMINISTRATOR, "Natasha", "Hall", riskScores, null);
        writePrivilege(out, employeeVertexId, APP_HR, "HR", EnumSet.of(AppPrivilege.READ, AppPrivilege.WRITE, AppPrivilege.ADMIN));

        employeeVertexId = "employee-0004";
        riskScores = new EmployeeRiskScores();
        riskScores.appAccounting = 0.1;
        riskScores.appInventory = 0.1;
        riskScores.appWiki = 0.3;
        riskScores.appHr = 0.0;
        riskScores.appWebsite = 0.0;
        riskScores.event401K = 0.1;
        riskScores.eventRemoteAccess = 0.2;
        writeEmployee(out, employeeVertexId, "POD423", TITLE_BUSINESS_ANALYST, "Ana", "Neal", riskScores, null);
        writePrivilege(out, employeeVertexId, APP_HR, "HR", EnumSet.of(AppPrivilege.READ, AppPrivilege.WRITE));

        employeeVertexId = "employee-0005";
        riskScores = new EmployeeRiskScores();
        riskScores.appAccounting = 0.3;
        riskScores.appInventory = 0.2;
        riskScores.appWiki = 0.1;
        riskScores.appHr = 0.0;
        riskScores.appWebsite = 0.3;
        riskScores.event401K = 0.1;
        riskScores.eventRemoteAccess = 0.4;
        writeEmployee(out, employeeVertexId, "IKD845", TITLE_BUSINESS_ANALYST, "Robin", "Norris", riskScores, null);

        employeeVertexId = "employee-0006";
        riskScores = new EmployeeRiskScores();
        riskScores.appAccounting = 0.1;
        riskScores.appInventory = 0.4;
        riskScores.appWiki = 0.0;
        riskScores.appHr = 0.2;
        riskScores.appWebsite = 0.0;
        riskScores.event401K = 0.3;
        riskScores.eventRemoteAccess = 0.6;
        writeEmployee(out, employeeVertexId, "NUP592", TITLE_BUSINESS_ANALYST, "Eugene", "Lindsey", riskScores, null);

        employeeVertexId = "employee-0007";
        riskScores = new EmployeeRiskScores();
        riskScores.appAccounting = 0.5;
        riskScores.appInventory = 0.2;
        riskScores.appWiki = 0.2;
        riskScores.appHr = 0.0;
        riskScores.appWebsite = 0.1;
        riskScores.event401K = 0.6;
        riskScores.eventRemoteAccess = 0.2;
        writeEmployee(out, employeeVertexId, "WJR923", TITLE_BUSINESS_ANALYST, "Kent", "Santiago", riskScores, null);

        employeeVertexId = "employee-0008";
        riskScores = new EmployeeRiskScores();
        riskScores.appAccounting = 0.2;
        riskScores.appInventory = 0.2;
        riskScores.appWiki = 0.2;
        riskScores.appHr = 0.0;
        riskScores.appWebsite = 0.1;
        riskScores.event401K = 0.6;
        riskScores.eventRemoteAccess = 0.2;
        writeEmployee(out, employeeVertexId, "WJR295", TITLE_BUSINESS_ANALYST, "Julio", "Malone", riskScores, null);
    }

    private void writeInsiderThreatEmployee(RdfWriter out) {
        String employeeVertexId = "employee-0001";
        EmployeeRiskScores riskScores = new EmployeeRiskScores();
        riskScores.appAccounting = 0.0;
        riskScores.appInventory = 0.8;
        riskScores.appWiki = 0.8;
        riskScores.appHr = 0.0;
        riskScores.appWebsite = 0.4;
        riskScores.event401K = 0.8;
        riskScores.eventRemoteAccess = 0.8;
        EmployeeAddress address = new EmployeeAddress();
        address.street1 = "8723 Camellia St.";
        address.street2 = "";
        address.city = "Ashburn";
        address.state = "VA";
        address.zipCode = "20147";
        address.generalGeoLocation = new GeoPoint(39.0398584, -77.4814385, "Home Address");
        address.geoLocation = new GeoPoint(39.0579026, -77.483009, "Home Address");
        writeEmployee(out, employeeVertexId, "AJK365", TITLE_BUSINESS_ANALYST, "Andrew", "Cooper", riskScores, address);
        writeInsiderThreatEmployee401KTransactions(out, employeeVertexId);
        writePrivilege(out, employeeVertexId, APP_WEB_SITE, "Website", EnumSet.of(AppPrivilege.READ, AppPrivilege.WRITE));
        writePrivilege(out, employeeVertexId, APP_WIKI, "Wiki", EnumSet.of(AppPrivilege.READ, AppPrivilege.WRITE));

        writePerformanceReviewEvent(out, employeeVertexId, new VisalloDateTime(2014, 11, 24, 10, 24, 22, 0, "EST"), PERFORMANCE_REVIEW_ACTION_REQUIRED, "Action Required Performance Review", 0.7);
        writeHrEvent(out, employeeVertexId, new VisalloDateTime(2015, 2, 9, 9, 24, 22, 0, "EST"), "Harassment", 0.9);
        writeHrEvent(out, employeeVertexId, new VisalloDateTime(2014, 11, 28, 9, 24, 22, 0, "EST"), "Unexcused Absence", 0.6);
        writeHrEvent(out, employeeVertexId, new VisalloDateTime(2014, 12, 22, 9, 24, 22, 0, "EST"), "Unexcused Absence", 0.6);
        writeHrEvent(out, employeeVertexId, new VisalloDateTime(2015, 1, 15, 9, 24, 22, 0, "EST"), "Unexcused Absence", 0.6);
        writeHrEvent(out, employeeVertexId, new VisalloDateTime(2015, 3, 23, 9, 24, 22, 0, "EST"), "Unexcused Absence", 0.6);
        writeHrEvent(out, employeeVertexId, new VisalloDateTime(2015, 7, 6, 9, 24, 22, 0, "EST"), "Unexcused Absence", 0.6);
        writeHrEvent(out, employeeVertexId, new VisalloDateTime(2015, 7, 7, 9, 24, 22, 0, "EST"), "Unexcused Absence", 0.6);

        writeRemoteAccess(out, employeeVertexId, new VisalloDateTime(2015, 7, 6, 22, 0, 12, 0, "EST"), 0.0, "Remote Access", REMOTE_ACCESS_IP_ADDRESS);
        writeApplicationEvent(out, employeeVertexId, APP_WEB_SITE, new VisalloDateTime(2014, 7, 6, 22, 1, 0, 0, "EST"), "Login", 0.0, REMOTE_ACCESS_IP_ADDRESS);
        writeApplicationEvent(out, employeeVertexId, APP_ACCOUNTING, new VisalloDateTime(2015, 7, 6, 22, 3, 12, 0, "EST"), "Failed Login", 0.6, REMOTE_ACCESS_IP_ADDRESS);
        writeApplicationEvent(out, employeeVertexId, APP_HR, new VisalloDateTime(2015, 7, 6, 22, 3, 12, 0, "EST"), "Failed Login", 0.8, REMOTE_ACCESS_IP_ADDRESS);
        writeRemoteAccess(out, "employee-0002", new VisalloDateTime(2015, 7, 6, 22, 10, 12, 0, "EST"), 0.7, "Remote Access Failed", REMOTE_ACCESS_IP_ADDRESS);
        writeRemoteAccess(out, "employee-0003", new VisalloDateTime(2015, 7, 6, 22, 20, 12, 0, "EST"), 0.7, "Remote Access Failed", REMOTE_ACCESS_IP_ADDRESS);
        writeRemoteAccess(out, "employee-0004", new VisalloDateTime(2015, 7, 6, 22, 30, 12, 0, "EST"), 0.7, "Remote Access Failed", REMOTE_ACCESS_IP_ADDRESS);
    }

    private void writeInsiderThreatEmployee401KTransactions(RdfWriter out, String employeeVertexId) {
        List<C1401KTransaction> transactions = new ArrayList<>();
        VisalloDateTime date = new VisalloDateTime(2014, 6, 1, 23, 59, 59, 0, "EST");
        Date endDate = new Date();
        while (date.toDateGMT().before(endDate)) {
            transactions.add(new C1401KTransaction(date, 583.0));
            date = date.add(14, VisalloDate.Unit.DAY);
        }
        transactions.add(new C1401KTransaction(new VisalloDateTime(2014, 8, 21, 16, 24, 15, 0, "EST"), -5000));
        transactions.add(new C1401KTransaction(new VisalloDateTime(2014, 10, 15, 16, 24, 15, 0, "EST"), -5000));
        transactions.add(new C1401KTransaction(new VisalloDateTime(2015, 1, 30, 16, 24, 15, 0, "EST"), -5000));
        transactions.add(new C1401KTransaction(new VisalloDateTime(2015, 2, 13, 16, 24, 15, 0, "EST"), -5000));
        transactions.add(new C1401KTransaction(new VisalloDateTime(2015, 3, 12, 16, 24, 15, 0, "EST"), -5000));
        transactions.add(new C1401KTransaction(new VisalloDateTime(2015, 4, 15, 16, 24, 15, 0, "EST"), -5000));
        transactions.add(new C1401KTransaction(new VisalloDateTime(2015, 4, 28, 16, 24, 15, 0, "EST"), -10000));
        transactions.add(new C1401KTransaction(new VisalloDateTime(2015, 5, 13, 16, 24, 15, 0, "EST"), -15000));
        transactions.add(new C1401KTransaction(new VisalloDateTime(2015, 5, 26, 16, 24, 15, 0, "EST"), -20000));
        transactions.add(new C1401KTransaction(new VisalloDateTime(2015, 6, 5, 16, 24, 15, 0, "EST"), -20000));
        Collections.sort(transactions);

        double balance = 80000.0;
        for (C1401KTransaction tx : transactions) {
            balance += tx.amount;
            write401KTransaction(out, employeeVertexId, tx.date, tx.amount, balance);
        }
    }

    private void writeRemoteAccess(RdfWriter out, String employeeVertexId, VisalloDateTime date, double riskScore, String riskReason, String ipAddress) {
        String vertexId = employeeVertexId + "-remoteAccess-" + date.toString();

        out.writeComment("Remote Access - " + ipAddress);
        out.writeConceptType(vertexId, "http://visallo.org/insider-threat#remoteAccess");
        out.writeValue(vertexId, "http://visallo.org/insider-threat#eventAction", riskReason);
        out.writeValue(vertexId, "http://visallo.org/insider-threat#eventDate", date);
        out.writeValue(vertexId, "http://visallo.org/insider-threat#ipAddress", ipAddress);
        out.writeValue(vertexId, "http://visallo.org/insider-threat#risk", riskScore);
        out.writeValue(vertexId, "http://visallo.org/insider-threat#riskReason", riskReason);
        out.writeRelationship(employeeVertexId, "http://visallo.org/insider-threat#hasEvent", vertexId);
        out.writeBlankLine();
    }

    private void writeTravel(RdfWriter out, String employeeVertexId, VisalloDateTime date, double riskScore, String riskReason, GeoPoint travelOrigin, GeoPoint travelDestination) {
        String vertexId = employeeVertexId + "-travelRequest-" + date.toString();

        out.writeComment("Travel Request");
        out.writeConceptType(vertexId, "http://visallo.org/insider-threat#travel");
        out.writeValue(vertexId, "http://visallo.org/insider-threat#eventAction", riskReason);
        out.writeValue(vertexId, "http://visallo.org/insider-threat#eventDate", date);
        out.writeValue(vertexId, "http://visallo.org/insider-threat#risk", riskScore);
        out.writeValue(vertexId, "http://visallo.org/insider-threat#riskReason", riskReason);
        out.writeValue(vertexId, "http://visallo.org/insider-threat#travelOrigin", travelOrigin);
        out.writeValue(vertexId, "http://visallo.org/insider-threat#travelDestination", travelDestination);
        out.writeRelationship(employeeVertexId, "http://visallo.org/insider-threat#hasEvent", vertexId);
        out.writeBlankLine();
    }

    private void writeTransferRequest(RdfWriter out, String employeeVertexId, VisalloDateTime date, double riskScore, String riskReason, GeoPoint travelOrigin, GeoPoint travelDestination) {
        String vertexId = employeeVertexId + "-transferRequest-" + date.toString();

        out.writeComment("Transfer Request");
        out.writeConceptType(vertexId, "http://visallo.org/insider-threat#transferRequest");
        out.writeValue(vertexId, "http://visallo.org/insider-threat#eventAction", riskReason);
        out.writeValue(vertexId, "http://visallo.org/insider-threat#eventDate", date);
        out.writeValue(vertexId, "http://visallo.org/insider-threat#risk", riskScore);
        out.writeValue(vertexId, "http://visallo.org/insider-threat#riskReason", riskReason);
        out.writeValue(vertexId, "http://visallo.org/insider-threat#travelOrigin", travelOrigin);
        out.writeValue(vertexId, "http://visallo.org/insider-threat#travelDestination", travelDestination);
        out.writeRelationship(employeeVertexId, "http://visallo.org/insider-threat#hasEvent", vertexId);
        out.writeBlankLine();
    }

    private void writeApplicationEvent(RdfWriter out, String employeeVertexId, String appVertexId, VisalloDateTime date, String title, double riskScore, String ipAddress) {
        String vertexId = employeeVertexId + "-" + appVertexId + date.toString();

        out.writeComment("Application Event");
        out.writeConceptType(vertexId, "http://visallo.org/insider-threat#applicationEvent");
        out.writeValue(vertexId, "http://visallo.org/insider-threat#eventAction", title);
        out.writeValue(vertexId, "http://visallo.org/insider-threat#eventDate", date);
        out.writeValue(vertexId, "http://visallo.org/insider-threat#risk", riskScore);
        out.writeValue(vertexId, "http://visallo.org/insider-threat#riskReason", title);
        out.writeValue(vertexId, "http://visallo.org/insider-threat#ipAddress", ipAddress);
        out.writeRelationship(employeeVertexId, "http://visallo.org/insider-threat#hasEvent", vertexId);
        out.writeRelationship(vertexId, "http://visallo.org/insider-threat#hasApplication", appVertexId);
        out.writeBlankLine();
    }

    private void writePerformanceReviewEvent(RdfWriter out, String employeeVertexId, VisalloDateTime date, String performanceReviewScore, String title, double riskScore) {
        String vertexId = employeeVertexId + "-" + date.toString();

        out.writeComment("Performance Review");
        out.writeConceptType(vertexId, "http://visallo.org/insider-threat#performanceReview");
        out.writeValue(vertexId, "http://visallo.org/insider-threat#eventAction", title);
        out.writeValue(vertexId, "http://visallo.org/insider-threat#eventDate", date);
        out.writeValue(vertexId, "http://visallo.org/insider-threat#risk", riskScore);
        out.writeValue(vertexId, "http://visallo.org/insider-threat#riskReason", title);
        out.writeValue(vertexId, "http://visallo.org/insider-threat#performanceReviewScore", performanceReviewScore);
        out.writeRelationship(employeeVertexId, "http://visallo.org/insider-threat#hasEvent", vertexId);
        out.writeBlankLine();
    }

    private void writeHrEvent(RdfWriter out, String employeeVertexId, VisalloDateTime date, String title, double riskScore) {
        String vertexId = employeeVertexId + "-" + date.toString();

        out.writeComment("HR Event");
        out.writeConceptType(vertexId, "http://visallo.org/insider-threat#hrEvent");
        out.writeValue(vertexId, "http://visallo.org/insider-threat#eventAction", title);
        out.writeValue(vertexId, "http://visallo.org/insider-threat#eventDate", date);
        out.writeValue(vertexId, "http://visallo.org/insider-threat#risk", riskScore);
        out.writeValue(vertexId, "http://visallo.org/insider-threat#riskReason", title);
        out.writeRelationship(employeeVertexId, "http://visallo.org/insider-threat#hasEvent", vertexId);
        out.writeBlankLine();
    }

    private void writePrivilege(RdfWriter out, String employeeVertexId, String appVertexId, String applicationName, EnumSet<AppPrivilege> appPrivileges) {
        String vertexId = employeeVertexId + "-" + appVertexId;
        out.writeComment(vertexId);
        out.writeConceptType(vertexId, "http://visallo.org/insider-threat#privilege");
        out.writeValue(vertexId, "http://visallo.org/insider-threat#applicationName", applicationName);
        for (AppPrivilege appPrivilege : appPrivileges) {
            out.writeValue(vertexId, "http://visallo.org/insider-threat#privilegeName:" + appPrivilege, appPrivilege.name());
            out.writeValue(vertexId, "http://visallo.org/insider-threat#risk:" + appPrivilege, appPrivilege.getRisk());
            out.writeValue(vertexId, "http://visallo.org/insider-threat#riskReason:" + appPrivilege, appPrivilege.getRiskReason());
        }
        out.writeRelationship(employeeVertexId, "http://visallo.org/insider-threat#hasPrivilege", vertexId);
        out.writeRelationship(appVertexId, "http://visallo.org/insider-threat#hasPrivilege", vertexId);
        out.writeBlankLine();
    }

    private void write401KTransaction(RdfWriter out, String employeeVertexId, VisalloDateTime date, double amount, double balance) {
        out.writeComment("401K Contribution for " + employeeVertexId + " - " + date);
        String vertexId = employeeVertexId + "-401K-" + date;
        out.writeConceptType(vertexId, IRI_CONCEPT_TYPE_401K_TRANSACTION);
        out.writeValue(vertexId, "http://visallo.org/insider-threat#transactionAmount", amount);
        out.writeValue(vertexId, "http://visallo.org/insider-threat#balance", balance);
        out.writeValue(vertexId, "http://visallo.org/insider-threat#eventDate", date);
        if (amount < 0) {
            out.writeValue(vertexId, IRI_PROPERTY_RISK, 0.8);
            out.writeValue(vertexId, IRI_PROPERTY_RISK_REASON, "Withdrawal from 401k");
        } else {
            out.writeValue(vertexId, IRI_PROPERTY_RISK, 0.0);
            out.writeValue(vertexId, IRI_PROPERTY_RISK_REASON, "401k Deposit");
        }
        out.writeRelationship(employeeVertexId, "http://visallo.org/insider-threat#hasEvent", vertexId);
        out.writeBlankLine();
    }

    private void writeEmployee(RdfWriter out, String vertexId, String userId, String title, String firstName, String lastName, EmployeeRiskScores riskScores, EmployeeAddress address) {
        String employeeId = vertexId;
        int i = employeeId.lastIndexOf('-');
        employeeId = employeeId.substring(i + 1);

        out.writeComment(firstName + " " + lastName);
        out.writeConceptType(vertexId, IRI_CONCEPT_TYPE_EMPLOYEE);
        out.writeValue(vertexId, "http://visallo.org/insider-threat#userId[HR]", userId);
        out.writeValue(vertexId, "http://visallo.org/insider-threat#employeeId", employeeId);
        out.writeValue(vertexId, "http://visallo.org/insider-threat#employeeTitle", title);
        out.writeValue(vertexId, IRI_PROPERTY_FIRST_NAME, firstName);
        out.writeValue(vertexId, IRI_PROPERTY_LAST_NAME, lastName);
        out.writeValue(vertexId, "http://visallo.org/insider-threat#applicationRisk:accounting", riskScores.appAccounting);
        out.writeValue(vertexId, "http://visallo.org/insider-threat#applicationRiskName:accounting", "Accounting");
        out.writeValue(vertexId, "http://visallo.org/insider-threat#applicationRisk:inventory", riskScores.appInventory);
        out.writeValue(vertexId, "http://visallo.org/insider-threat#applicationRiskName:inventory", "Inventory");
        out.writeValue(vertexId, "http://visallo.org/insider-threat#applicationRisk:wiki", riskScores.appWiki);
        out.writeValue(vertexId, "http://visallo.org/insider-threat#applicationRiskName:wiki", "Wiki");
        out.writeValue(vertexId, "http://visallo.org/insider-threat#applicationRisk:hr", riskScores.appHr);
        out.writeValue(vertexId, "http://visallo.org/insider-threat#applicationRiskName:hr", "HR");
        out.writeValue(vertexId, "http://visallo.org/insider-threat#applicationRisk:website", riskScores.appWebsite);
        out.writeValue(vertexId, "http://visallo.org/insider-threat#applicationRiskName:website", "Website");
        out.writeValue(vertexId, "http://visallo.org/insider-threat#totalApplicationRisk", riskScores.getTotalApplicationRisk());

        out.writeValue(vertexId, "http://visallo.org/insider-threat#eventRisk:401k", riskScores.event401K);
        out.writeValue(vertexId, "http://visallo.org/insider-threat#eventRiskName:401k", "401K");
        out.writeValue(vertexId, "http://visallo.org/insider-threat#eventRisk:remote-access", riskScores.eventRemoteAccess);
        out.writeValue(vertexId, "http://visallo.org/insider-threat#eventRiskName:remote-access", "401K");
        out.writeValue(vertexId, "http://visallo.org/insider-threat#totalEventRisk", riskScores.getTotalEventRisk());

        out.writeValue(vertexId, "http://visallo.org/insider-threat#totalRisk", riskScores.getTotalRisk());

        if (address != null) {
            out.writeValue(vertexId, "http://visallo.org/insider-threat#street1[HR]", address.street1);
            out.writeValue(vertexId, "http://visallo.org/insider-threat#street2[HR]", address.street2);
            out.writeValue(vertexId, "http://visallo.org/insider-threat#city", address.city);
            out.writeValue(vertexId, "http://visallo.org/insider-threat#state", address.state);
            out.writeValue(vertexId, "http://visallo.org/insider-threat#zipCode", address.zipCode);
            out.writeValue(vertexId, "http://visallo.org/insider-threat#geoLocation", address.generalGeoLocation);
            out.writeValue(vertexId, "http://visallo.org/insider-threat#geoLocation[HR]", address.geoLocation);
        }

        String imageVertexId = vertexId + "-image";
        String imageVertexIdWithVisibility = imageVertexId + "[HR]";
        out.writeConceptType(imageVertexIdWithVisibility, "http://visallo.org/insider-threat#media");
        out.writeValue(imageVertexIdWithVisibility, "http://visallo.org#title[HR]", "Image of " + lastName + ", " + firstName);
        out.writeStreamingPropertyValue(imageVertexIdWithVisibility, "http://visallo.org#raw[HR]", vertexId + ".jpg");
        out.writeRelationship(vertexId, "http://visallo.org/cardTransaction#entityHasMedia[HR]", vertexId + "-image");
        out.writeValue(vertexId, "http://visallo.org#entityImageVertexId[HR]", imageVertexId);

        out.writeBlankLine();
    }

    private void writeApplications(RdfWriter out) {
        writeApplication(out, APP_ACCOUNTING, "Accounting");
        writeApplication(out, APP_INVENTORY, "Inventory");
        writeApplication(out, APP_WIKI, "Wiki");
        writeApplication(out, APP_HR, "HR");
        writeApplication(out, APP_WEB_SITE, "Website");
    }

    private void writeApplication(RdfWriter out, String applicationId, String applicationName) {
        out.writeComment("Application: " + applicationName);
        out.writeConceptType(applicationId, IRI_CONCEPT_TYPE_APPLICATION);
        out.writeValue(applicationId, IRI_PROPERTY_ASSET_TITLE, applicationName);
        out.writeBlankLine();
    }

    private static class C1401KTransaction implements Comparable<C1401KTransaction> {
        public final VisalloDateTime date;
        public final double amount;

        public C1401KTransaction(VisalloDateTime date, double amount) {
            this.date = date;
            this.amount = amount;
        }

        @Override
        public int compareTo(C1401KTransaction o) {
            return this.date.compareTo(o.date);
        }
    }

    private static class RdfWriter {
        private final PrintWriter writer;

        public RdfWriter(PrintWriter writer) {
            this.writer = writer;
        }

        public void writeConceptType(String vertexId, String iri) {
            writeRelationship(vertexId, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", iri);
        }

        public void writeRelationship(String v1, String label, String v2) {
            writer.println(String.format("<%s> <%s> <%s>", v1, label, v2));
        }

        public void writeValue(String vertexId, String propertyIri, String value) {
            writer.println(String.format("<%s> <%s> \"%s\"", vertexId, propertyIri, value));
        }

        public void writeValue(String vertexId, String propertyIri, GeoPoint value) {
            String valueString = String.format("%s [%f, %f]", value.getDescription(), value.getLatitude(), value.getLongitude());
            writer.println(String.format("<%s> <%s> \"%s\"^^<http://visallo.org#geolocation>", vertexId, propertyIri, valueString));
        }

        public void writeValue(String vertexId, String propertyIri, double value) {
            writer.println(String.format("<%s> <%s> \"%f\"^^<http://www.w3.org/2001/XMLSchema#double>", vertexId, propertyIri, value));
        }

        public void writeValue(String vertexId, String propertyIri, VisalloDateTime value) {
            writer.println(String.format("<%s> <%s> \"%s\"^^<http://www.w3.org/2001/XMLSchema#dateTime>", vertexId, propertyIri, value.toString()));
        }

        public void close() {
            this.writer.close();
        }

        public void writeComment(String str) {
            writer.println(String.format("# %s", str));
        }

        public void writeBlankLine() {
            writer.println();
        }

        public void writeStreamingPropertyValue(String vertexId, String propertyIri, String value) {
            writer.println(String.format("<%s> <%s> \"%s\"^^<http://visallo.org#streamingPropertyValue>", vertexId, propertyIri, value));
        }
    }

    public enum AppPrivilege {
        READ(0.1, "Read Access"),
        WRITE(0.6, "Write Access"),
        ADMIN(0.8, "Admin Access");

        private double risk;
        private String riskReason;

        AppPrivilege(double risk, String riskReason) {
            this.risk = risk;
            this.riskReason = riskReason;
        }

        public double getRisk() {
            return risk;
        }

        public String getRiskReason() {
            return riskReason;
        }
    }

    public static class EmployeeRiskScores {
        public double appAccounting;
        public double appInventory;
        public double appWiki;
        public double appHr;
        public double appWebsite;
        public double event401K;
        public double eventRemoteAccess;

        public double getTotalApplicationRisk() {
            return (appAccounting + appInventory + appWiki + appHr + appWebsite) / 5.0;
        }

        public double getTotalEventRisk() {
            return (event401K + eventRemoteAccess) / 2.0;
        }

        public double getTotalRisk() {
            return (getTotalApplicationRisk() + getTotalEventRisk()) / 2.0;
        }
    }

    public static class EmployeeAddress {
        public String street1;
        public String street2;
        public String city;
        public String state;
        public String zipCode;
        public GeoPoint generalGeoLocation;
        public GeoPoint geoLocation;
    }
}
