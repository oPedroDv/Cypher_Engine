package com.cypher.analysis.engine;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Component
@ConfigurationProperties(prefix = "cypher.risk")
public class RiskEngineConfig {
    private double issuerHighThreshold = 0.15;
    private double issuerCriticalThreshold = 0.30;
    private double issuerHighFloor = 0.70;
    private double issuerCriticalFloor = 0.85;
    private double payerHighThreshold = 0.10;
    private double payerCriticalThreshold = 0.25;
    private double payerHighFloor = 0.70;
    private double payerCriticalFloor = 0.90;
    private double valueAnomalyHighRatio = 5.0;
    private double valueAnomalyCriticalRatio = 8.0;
    private double valueAnomalyHighFloor = 0.65;
    private double valueAnomalyCriticalFloor = 0.75;
    private double defaultAdvanceRatio = 0.90;
    private double pairHighThreshold = 0.05;
    private double pairCriticalThreshold = 0.15;
    private double pairHighFloor = 0.65;
    private double pairCriticalFloor = 0.85;
    private double payerLateThreshold = 0.30;
    private double payerLateFloor = 0.60;
    private double sourceUnavailablePenalty = 0.10;
    private double sourceUnavailableFloor = 0.35;
    private double externalValidationFloor = 0.20;
    private double externalValidationUnknownFloor = 0.25;
    private double issuerClosedFloor = 0.90;
    private double payerClosedFloor = 0.80;
    private double issuerUnfitFloor = 0.80;
    private double payerUnfitFloor = 0.70;
    private double issuerSuspendedFloor = 0.65;
    private double payerSuspendedFloor = 0.55;
    private double sefazBlockedFloor = 0.95;
    private double sefazPendingFloor = 0.70;
    private double sefazUnavailableFloor = 0.45;
    private double missingMaturityFloor = 0.40;
    private double overdueMaturityFloor = 0.75;
    private int nearMaturityDays = 3;
    private double nearMaturityFloor = 0.60;
    private double financialLossMultiplier = 0.18;
    private double financialAdvanceHaircut = 0.15;
    private double financialMaxAdvanceRatio = 0.95;
    private double financialRateRiskPremium = 2.50;

    public double getIssuerHighThreshold() { return issuerHighThreshold; }
    public void setIssuerHighThreshold(double value) { issuerHighThreshold = value; }
    public double getIssuerCriticalThreshold() { return issuerCriticalThreshold; }
    public void setIssuerCriticalThreshold(double value) { issuerCriticalThreshold = value; }
    public double getIssuerHighFloor() { return issuerHighFloor; }
    public void setIssuerHighFloor(double value) { issuerHighFloor = value; }
    public double getIssuerCriticalFloor() { return issuerCriticalFloor; }
    public void setIssuerCriticalFloor(double value) { issuerCriticalFloor = value; }
    public double getPayerHighThreshold() { return payerHighThreshold; }
    public void setPayerHighThreshold(double value) { payerHighThreshold = value; }
    public double getPayerCriticalThreshold() { return payerCriticalThreshold; }
    public void setPayerCriticalThreshold(double value) { payerCriticalThreshold = value; }
    public double getPayerHighFloor() { return payerHighFloor; }
    public void setPayerHighFloor(double value) { payerHighFloor = value; }
    public double getPayerCriticalFloor() { return payerCriticalFloor; }
    public void setPayerCriticalFloor(double value) { payerCriticalFloor = value; }
    public double getValueAnomalyHighRatio() { return valueAnomalyHighRatio; }
    public void setValueAnomalyHighRatio(double value) { valueAnomalyHighRatio = value; }
    public double getValueAnomalyCriticalRatio() { return valueAnomalyCriticalRatio; }
    public void setValueAnomalyCriticalRatio(double value) { valueAnomalyCriticalRatio = value; }
    public double getValueAnomalyHighFloor() { return valueAnomalyHighFloor; }
    public void setValueAnomalyHighFloor(double value) { valueAnomalyHighFloor = value; }
    public double getValueAnomalyCriticalFloor() { return valueAnomalyCriticalFloor; }
    public void setValueAnomalyCriticalFloor(double value) { valueAnomalyCriticalFloor = value; }
    public double getDefaultAdvanceRatio() { return defaultAdvanceRatio; }
    public void setDefaultAdvanceRatio(double value) { defaultAdvanceRatio = value; }
    public double getPairHighThreshold() { return pairHighThreshold; }
    public void setPairHighThreshold(double value) { pairHighThreshold = value; }
    public double getPairCriticalThreshold() { return pairCriticalThreshold; }
    public void setPairCriticalThreshold(double value) { pairCriticalThreshold = value; }
    public double getPairHighFloor() { return pairHighFloor; }
    public void setPairHighFloor(double value) { pairHighFloor = value; }
    public double getPairCriticalFloor() { return pairCriticalFloor; }
    public void setPairCriticalFloor(double value) { pairCriticalFloor = value; }
    public double getPayerLateThreshold() { return payerLateThreshold; }
    public void setPayerLateThreshold(double value) { payerLateThreshold = value; }
    public double getPayerLateFloor() { return payerLateFloor; }
    public void setPayerLateFloor(double value) { payerLateFloor = value; }
    public double getSourceUnavailablePenalty() { return sourceUnavailablePenalty; }
    public void setSourceUnavailablePenalty(double value) { sourceUnavailablePenalty = value; }
    public double getSourceUnavailableFloor() { return sourceUnavailableFloor; }
    public void setSourceUnavailableFloor(double value) { sourceUnavailableFloor = value; }
    public double getExternalValidationFloor() { return externalValidationFloor; }
    public void setExternalValidationFloor(double value) { externalValidationFloor = value; }
    public double getExternalValidationUnknownFloor() { return externalValidationUnknownFloor; }
    public void setExternalValidationUnknownFloor(double value) { externalValidationUnknownFloor = value; }
    public double getIssuerClosedFloor() { return issuerClosedFloor; }
    public void setIssuerClosedFloor(double value) { issuerClosedFloor = value; }
    public double getPayerClosedFloor() { return payerClosedFloor; }
    public void setPayerClosedFloor(double value) { payerClosedFloor = value; }
    public double getIssuerUnfitFloor() { return issuerUnfitFloor; }
    public void setIssuerUnfitFloor(double value) { issuerUnfitFloor = value; }
    public double getPayerUnfitFloor() { return payerUnfitFloor; }
    public void setPayerUnfitFloor(double value) { payerUnfitFloor = value; }
    public double getIssuerSuspendedFloor() { return issuerSuspendedFloor; }
    public void setIssuerSuspendedFloor(double value) { issuerSuspendedFloor = value; }
    public double getPayerSuspendedFloor() { return payerSuspendedFloor; }
    public void setPayerSuspendedFloor(double value) { payerSuspendedFloor = value; }
    public double getSefazBlockedFloor() { return sefazBlockedFloor; }
    public void setSefazBlockedFloor(double value) { sefazBlockedFloor = value; }
    public double getSefazPendingFloor() { return sefazPendingFloor; }
    public void setSefazPendingFloor(double value) { sefazPendingFloor = value; }
    public double getSefazUnavailableFloor() { return sefazUnavailableFloor; }
    public void setSefazUnavailableFloor(double value) { sefazUnavailableFloor = value; }
    public double getMissingMaturityFloor() { return missingMaturityFloor; }
    public void setMissingMaturityFloor(double value) { missingMaturityFloor = value; }
    public double getOverdueMaturityFloor() { return overdueMaturityFloor; }
    public void setOverdueMaturityFloor(double value) { overdueMaturityFloor = value; }
    public int getNearMaturityDays() { return nearMaturityDays; }
    public void setNearMaturityDays(int value) { nearMaturityDays = value; }
    public double getNearMaturityFloor() { return nearMaturityFloor; }
    public void setNearMaturityFloor(double value) { nearMaturityFloor = value; }
    public double getFinancialLossMultiplier() { return financialLossMultiplier; }
    public void setFinancialLossMultiplier(double value) { financialLossMultiplier = value; }
    public double getFinancialAdvanceHaircut() { return financialAdvanceHaircut; }
    public void setFinancialAdvanceHaircut(double value) { financialAdvanceHaircut = value; }
    public double getFinancialMaxAdvanceRatio() { return financialMaxAdvanceRatio; }
    public void setFinancialMaxAdvanceRatio(double value) { financialMaxAdvanceRatio = value; }
    public double getFinancialRateRiskPremium() { return financialRateRiskPremium; }
    public void setFinancialRateRiskPremium(double value) { financialRateRiskPremium = value; }
}
