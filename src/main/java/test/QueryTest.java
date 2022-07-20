package test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AddFieldsOperation;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SpringBootTest(classes = TestApplication.class)
public class QueryTest {
    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    @Transactional
    public void initData() {
        mongoTemplate.dropCollection(PersonPo.class);

        PersonPo mother = new PersonPo();
        mother.setName("Nancy");
        mongoTemplate.save(mother);

        PersonPo father = new PersonPo();
        father.setName("Aray");
        mongoTemplate.save(father);

        PersonPo chris = new PersonPo();
        chris.setName("Chris");
        chris.setFather(father);
        chris.setMother(mother);
        mongoTemplate.save(chris);
    }

    @Test
    public void test() {
        // query any person whose mother's name is Nancy
        // similar as following pseudo SQL
        // select * from person as p left join person as mother on p.mother.id=mother.id where mother.name='Nancy'

        AddFieldsOperation addMotherId = Aggregation.addFields().addField("motherId")
                .withValue("$mother.$id")
                .build();
        AggregationOperation motherLookupDoesNotWork = Aggregation.lookup(
                "person",
                "mother.$id",
                "_id",
                "mother"
        );
        AggregationOperation motherLookupWorks = Aggregation.lookup(
                "person",
                "motherId",
                "_id",
                "mother"
        );

        UnwindOperation unwind = Aggregation.unwind("mother");
        AggregationOperation where = Aggregation.match(Criteria.where("mother.name").is("Nancy"));

        // The following block works.
        {
            // adding a (virtual) field "motherId" and using this newly added field. It works.
/*
            [{
                $addFields: {
                    motherId: '$mother.$id'
                }
            }, {
                $lookup: {
                    from: 'person',
                    localField: 'motherId',
                    foreignField: '_id',
                    as: 'mother'
                }
            }, {
                $unwind: {
                    path: '$mother'
                }
            }, {
                $match: {
                    $expr: {
                        $and: {
                            $eq: [
                                '$mother.name',
                                'Nancy'
                            ]
                        }
                    }
                }
            }]
*/
            Aggregation aggregationWorks = Aggregation.newAggregation(addMotherId, motherLookupWorks, unwind, where);
            List<PersonPo> result = mongoTemplate.aggregate(aggregationWorks, mongoTemplate.getCollectionName(PersonPo.class), PersonPo.class).getMappedResults();
            Assertions.assertEquals(result.size(), 1);
        }

        // The following block does NOT work.
        {
            //       directly use "mother.$id", it works in MongoDb Compass as following, but it does NOT work in Java codes.
/*
            [{
                $lookup: {
                    from: 'person',
                    localField: 'mother.$id',
                    foreignField: '_id',
                    as: 'mother'
                }
            }, {
                $unwind: {
                    path: '$mother'
                }
            }, {
                $match: {
                    $expr: {
                        $and: {
                            $eq: [
                                '$mother.name',
                                'Nancy'
                            ]
                        }
                    }
                }
            }]
*/
            Aggregation aggregationDoesNotWork = Aggregation.newAggregation(motherLookupDoesNotWork, unwind, where);
            List<PersonPo> emptyResult = mongoTemplate.aggregate(aggregationDoesNotWork, mongoTemplate.getCollectionName(PersonPo.class), PersonPo.class).getMappedResults();
            Assertions.assertEquals(emptyResult.size(), 1);
        }

    }
}
