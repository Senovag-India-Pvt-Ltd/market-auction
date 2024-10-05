package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.model.entity.PupaTestAndCocoonAssessment;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.time.LocalDate;
import java.util.List;

public interface PupaTestAndCocoonAssessmentRepository extends PagingAndSortingRepository<PupaTestAndCocoonAssessment, Integer> {

    public PupaTestAndCocoonAssessment save(PupaTestAndCocoonAssessment pupaTestAndCocoonAssessment);

    public List<PupaTestAndCocoonAssessment> findAllByPupaTestResult(String pupaTestResult);

    public List<PupaTestAndCocoonAssessment> findAllByCocoonAssessmentResult(String cocoonAssessmentResult);

    public List<PupaTestAndCocoonAssessment> findAllByCocoonAssessmentResultAndPupaTestResult(String cocoonAssessmentResult, String pupatestResult);

    public PupaTestAndCocoonAssessment findByMarketAuctionId(int marketAuctionId);

    public List<PupaTestAndCocoonAssessment> findAllByTestDate(LocalDate testDate);
}
