package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.model.entity.TransactionFileGeneration;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionFileGenRepository extends CrudRepository<TransactionFileGeneration, Integer> {

    public TransactionFileGeneration save(TransactionFileGeneration at) ;

    @Query("select tfg from TransactionFileGeneration tfg where tfg.status in ('generated') order by createdDate limit 5")
    public List<TransactionFileGeneration> getRowsByStatusAsGenerated();

    @Query("select tfg from TransactionFileGeneration tfg where tfg.status in ('pushed')  order by createdDate limit 5")
    public List<TransactionFileGeneration> getRowsByStatusAsPushed();

    @Query("select tfg from TransactionFileGeneration tfg where tfg.transactionFileGenId=:transactionFileGenId")
    public TransactionFileGeneration getRowForCSV(String transactionFileGenId);

}