package it.redhat.dgb.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.redhat.dgb.model.Report;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.Search;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;

import io.quarkus.infinispan.client.Remote;
import io.quarkus.logging.Log;
import it.redhat.dgb.model.SftRec;
import it.redhat.dgb.model.TimeFormatter;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

@Path("query")
public class QueryResource {

   @Inject
   @Remote("sftrec")
   RemoteCache<String, SftRec> sftrecCache;

   @GET
   @Produces("application/json")
   public List<SftRec> simple() {
      Query<SftRec> query = Search.getQueryFactory(sftrecCache)
            .create("from it.redhat.dgb.SftRec s WHERE s.reporting_timestamp = 1681250400000");
      query.maxResults(100);

      List<SftRec> entries = query.execute().list();
      Log.info(entries);
      return entries;
   }

   @GET
   @Path("benchmark/{cacheSize}")
   @Produces("application/json")
   public List<Report> benchmark(@PathParam("cacheSize") int cacheSize) {
      List<Report> reports = new ArrayList<Report>();
      StringBuffer buffer = new StringBuffer();
      long startQ, endQ;
      for (Map.Entry<String, String> entry : queryString().entrySet()) {
         Report report = new Report();
         report.cache_size = cacheSize;
         report.description = entry.getKey();
         QueryFactory qf = Search.getQueryFactory(sftrecCache);
         Query<SftRec> query = qf.create(queryString().get(entry.getKey()));
         startQ = System.currentTimeMillis();
         List<SftRec> recs = query.execute().list();
         endQ = System.currentTimeMillis();
         report.duration = endQ - startQ;
         report.query_size = recs.size();
         reports.add(report);
         buffer.append(entry.getKey()).append(",").append(cacheSize).app
         Log.info("===> query report: " + report);
      }
      return reports;
   }

   @GET
   @Path("base")
   @Produces("application/json")
   public List<SftRec> base(@QueryParam("fromDate") long fromDate, @QueryParam("endDate") long endDate) {
      Query<SftRec> query = Search.getQueryFactory(sftrecCache)
            .create("from it.redhat.dgb.SftRec s WHERE s.received_Report_Date >= :fromDate AND s.received_Report_Date <= :endDate");
      query.maxResults(100);
      query.setParameter("fromDate", fromDate);
      query.setParameter("endDate", endDate);

      List<SftRec> entries = query.execute().list();
      Log.info(entries);
      return entries;
   }

   @GET
   @Path("aggr")
   @Produces("application/json")
   public List<Object[]> aggregated(@QueryParam("fromDate") long fromDate, @QueryParam("endDate") long endDate) {
      Query<Object[]> query = Search.getQueryFactory(sftrecCache)
            .create("select count(uTI) from it.redhat.dgb.SftRec s WHERE s.received_Report_Date >= :fromDate AND s.received_Report_Date <= :endDate");
      query.maxResults(100);
      query.setParameter("fromDate", fromDate);
      query.setParameter("endDate", endDate);

      //List<SftRec> entries = query.execute().list();
      List<Object[]> reslt = query.execute().list();
      Log.info(reslt);
      return reslt;
   }

   @GET
   @Path("{type}")
   @Produces("application/json")
   public List<SftRec> query(@PathParam("type") String type) {
      // Remote Query, using protobuf
      QueryFactory qf = Search.getQueryFactory(sftrecCache);
      Query<SftRec> query = qf.create(queryString().get(type));
      long startQuery = System.currentTimeMillis();
      List<SftRec> recs = query.execute().list();
      long endQuery = System.currentTimeMillis();

      Log.info("Aggregated Query type 1 time of execution: " + TimeFormatter.print(endQuery - startQuery));
      return recs;
   }

   private static Map<String, String> queryString(){
      HashMap<String, String> queries = new HashMap<String, String>();
      queries.put("RPS", "SELECT report_status, COUNT(uTI) FROM it.redhat.dgb.SftRec GROUP BY report_status ORDER BY report_status");
      queries.put("MTS", "SELECT matching_status, COUNT(uTI) FROM it.redhat.dgb.SftRec GROUP BY matching_status ORDER BY matching_status");
      queries.put("LRS", "SELECT loan_reconciliatio_n_status, COUNT(uTI) FROM it.redhat.dgb.SftRec GROUP BY loan_reconciliatio_n_status ORDER BY loan_reconciliatio_n_status");
      queries.put("CRS", "SELECT collateral_reconciliation_status, COUNT(uTI) FROM it.redhat.dgb.SftRec GROUP BY collateral_reconciliation_status ORDER BY collateral_reconciliation_status");
      queries.put("RPS_NO_COUNT", "SELECT report_status FROM it.redhat.dgb.SftRec GROUP BY report_status ORDER BY report_status");
      queries.put("MTS_NO_COUNT", "SELECT matching_status FROM it.redhat.dgb.SftRec GROUP BY matching_status ORDER BY matching_status");
      queries.put("LRS_NO_COUNT", "SELECT loan_reconciliatio_n_status FROM it.redhat.dgb.SftRec GROUP BY loan_reconciliatio_n_status ORDER BY loan_reconciliatio_n_status");
      queries.put("CRS_NO_COUNT", "SELECT collateral_reconciliation_status FROM it.redhat.dgb.SftRec GROUP BY collateral_reconciliation_status ORDER BY collateral_reconciliation_status");
      queries.put("RPS_ONE_DAY", "SELECT report_status, COUNT(uTI) FROM it.redhat.dgb.SftRec WHERE received_Report_Date >= 1672873200000 AND received_Report_Date <= 1672873200000 GROUP BY report_status ORDER BY report_status");
      queries.put("MTS_ONE_DAY", "SELECT matching_status, COUNT(uTI) FROM it.redhat.dgb.SftRec WHERE received_Report_Date >= 1672873200000 AND received_Report_Date <= 1672873200000 GROUP BY matching_status ORDER BY matching_status");
      queries.put("LRS_ONE_DAY", "SELECT loan_reconciliatio_n_status, COUNT(uTI) FROM it.redhat.dgb.SftRec WHERE received_Report_Date >= 1672873200000 AND received_Report_Date <= 1672873200000 GROUP BY loan_reconciliatio_n_status ORDER BY loan_reconciliatio_n_status");
      queries.put("CRS_ONE_DAY", "SELECT collateral_reconciliation_status, COUNT(uTI) FROM it.redhat.dgb.SftRec WHERE received_Report_Date >= 1672873200000 AND received_Report_Date <= 1672873200000 GROUP BY collateral_reconciliation_status ORDER BY collateral_reconciliation_status");
      return queries;
   }

}
